/*
 * Copyright 2022 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.mysql.dao;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.github.drinkjava2.jdialects.Dialect;
import com.google.common.base.CaseFormat;
import com.netflix.conductor.common.metadata.events.EventExecution;
import com.netflix.conductor.common.metadata.tasks.TaskExecLog;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.TaskSummary;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.exception.NotFoundException;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.IndexDAO;
import com.netflix.conductor.metrics.Monitors;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

@ConditionalOnProperty(name = "conductor.indexing.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class DBIndexDAO implements IndexDAO {
	private static final String ARCHIVED_FIELD = "archived";
	private Dialect dialect = null;
	
    public DBIndexDAO(JdbcTemplate jdbcTemplate, ExecutionDAO executionDAO) {
    	this.jdbcTemplate = jdbcTemplate;
    	this.executionDAO = executionDAO;
    	this.dialect = Dialect.guessDialect(jdbcTemplate.getDataSource());
    	int corePoolSize = 1;
    	int maximumPoolSize = 20;
        long keepAliveTime = 30L;
        int workerQueueSize = 1000;
        
        this.executorService = new ThreadPoolExecutor(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(workerQueueSize),
                (runnable, executor) -> {
                    LOGGER.warn("Request  {} to async dao discarded in executor {}", runnable, executor);
                    Monitors.recordDiscardedIndexingCount("indexQueue");
                });
        
    	this.logExecutorService = new ThreadPoolExecutor(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(workerQueueSize),
                (runnable, executor) -> {
                    LOGGER.warn("Request {} to async log dao discarded in executor {}", runnable, executor);
                    Monitors.recordDiscardedIndexingCount("logQueue");
                });
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DBIndexDAO.class);

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMWW");

    private ExecutorService executorService;
    private ExecutorService logExecutorService;
    private JdbcTemplate jdbcTemplate;
    private ExecutionDAO executionDAO;
    
    
    static {
        SIMPLE_DATE_FORMAT.setTimeZone(GMT);
    }

    @PreDestroy
    private void shutdown() {
        LOGGER.info("Gracefully shutdown executor service");
        shutdownExecutorService(logExecutorService);
        shutdownExecutorService(executorService);
    }

    private void shutdownExecutorService(ExecutorService execService) {
        try {
            execService.shutdown();
            if (execService.awaitTermination(30, TimeUnit.SECONDS)) {
                LOGGER.debug("tasks completed, shutting down");
            } else {
                LOGGER.warn("Forcing shutdown after waiting for 30 seconds");
                execService.shutdownNow();
            }
        } catch (InterruptedException ie) {
            LOGGER.warn(
                    "Shutdown interrupted, invoking shutdownNow on scheduledThreadPoolExecutor for delay queue");
            execService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void indexWorkflow(WorkflowSummary workflow) {
    	this.removeWorkflowNotIncludeTask(workflow.getWorkflowId());
    	
    	Timestamp startTimeTp = convertTimestamp(workflow.getStartTime());
    	Timestamp updateTimeTp = convertTimestamp(workflow.getUpdateTime());
    	Timestamp endTimeTp = convertTimestamp(workflow.getEndTime());
    	String status = workflow.getStatus().toString();
    	String insertWorkflowSql = "insert into index_workflow (workflow_id, correlation_id, workflow_type, workflow_version, reason_for_incompletion, status,json_data, execution_time, start_time, end_time, update_time)" 
    				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    	jdbcTemplate.update(insertWorkflowSql,workflow.getWorkflowId(),workflow.getCorrelationId(),
    			workflow.getWorkflowType(),workflow.getVersion(),workflow.getReasonForIncompletion(), status, 
    			"{\"input\":" + workflow.getInput() + ",\"output\":" + workflow.getOutput() + "}",
    			workflow.getExecutionTime(), startTimeTp,updateTimeTp,endTimeTp);
    }

    private Timestamp convertTimestamp(String dateTime) {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    	sdf.setTimeZone(GMT);
		try {
			if(StringUtils.isNotBlank(dateTime)) {
				Date startTime = sdf.parse(dateTime);
		    	return new Timestamp(startTime.getTime());
			}	
	    	
		} catch (ParseException e) {
			LOGGER.warn("convert time error while index workflow",e);
		}
		return null;
    }
    
    @Override
    public CompletableFuture<Void> asyncIndexWorkflow(WorkflowSummary workflow) {
        return CompletableFuture.runAsync(() -> indexWorkflow(workflow), executorService);
    }

    @Override
    public void indexTask(TaskSummary task) {
    	jdbcTemplate.update("delete from index_task where task_id = ?",task.getTaskId());
    	
    	Timestamp scheduledTimeTp = convertTimestamp(task.getScheduledTime());
    	Timestamp startTimeTp = convertTimestamp(task.getStartTime());
    	Timestamp updateTimeTp = convertTimestamp(task.getUpdateTime());
    	Timestamp endTimeTp = convertTimestamp(task.getEndTime());
    	String status = task.getStatus().toString();
    	
    	String insertWorkflowSql = "insert into index_task (task_id, workflow_id, correlation_id, workflow_type, workflow_priority, task_def_name, task_type"
    			+ ", domain, execution_time, queue_wait_time, reason_for_incompletion, status, json_data, scheduled_time, start_time, end_time, update_time)" 
    				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    	jdbcTemplate.update(insertWorkflowSql,task.getTaskId(), task.getWorkflowId(),task.getCorrelationId(),
    			task.getWorkflowType(), task.getWorkflowPriority(), task.getTaskDefName(), task.getTaskType(),task.getDomain(),
    			task.getExecutionTime(), task.getQueueWaitTime(),  task.getReasonForIncompletion(), status, 
    			"{\"input\":" + task.getInput() + ",\"output\":" + task.getOutput() + "}",
    			scheduledTimeTp, startTimeTp, updateTimeTp, endTimeTp);
    	
    }

    @Override
    public CompletableFuture<Void> asyncIndexTask(TaskSummary task) {
        return CompletableFuture.runAsync(() -> indexTask(task), executorService);
    }

    @Override
    public void addTaskExecutionLogs(List<TaskExecLog> taskExecLogs) {
    	String insertTaskExecLogSql = "insert into index_task_execution_log (task_id, log, created_time) VALUES (?, ?, ?, ?)";
    	for (TaskExecLog taskExecLog : taskExecLogs) {
			jdbcTemplate.update(insertTaskExecLogSql, taskExecLog.getTaskId(), taskExecLog.getLog(), taskExecLog.getCreatedTime());
		}
    }

    @Override
    public CompletableFuture<Void> asyncAddTaskExecutionLogs(List<TaskExecLog> logs) {
        return CompletableFuture.runAsync(() -> addTaskExecutionLogs(logs), logExecutorService);
    }

    @Override
    public List<TaskExecLog> getTaskExecutionLogs(String taskId) {
    	List<TaskExecLog> logs = jdbcTemplate.queryForList("select * from index_task_execution_log where task_id = ?", TaskExecLog.class,taskId);
        return logs;
    }

    @Override
    public List<Message> getMessages(String queue) {
    	List<Message> messages = jdbcTemplate.queryForList("select message_id as id,popped as receipt,payload,priority from queue_message where queue_name = ?", Message.class,queue);
        return messages;
    }

    @Override
    public List<EventExecution> getEventExecutions(String event) {
    	//暂不实现
        return null;
    }
    
    @Override
    public void addMessage(String queue, Message message) {
    	//不需要实现
    }

    @Override
    public CompletableFuture<Void> asyncAddMessage(String queue, Message message) {
        return CompletableFuture.runAsync(() -> addMessage(queue, message), executorService);
    }

    @Override
    public void addEventExecution(EventExecution eventExecution) {
    	//暂不实现
    }

    @Override
    public CompletableFuture<Void> asyncAddEventExecution(EventExecution eventExecution) {
        return CompletableFuture.runAsync(
                () -> addEventExecution(eventExecution), logExecutorService);
    }

    /**
     * String query = "correlationId='" + correlationId + "' AND workflowType='" + workflowName +
     * "'"; String freeText = "*";
     * start=0&size=15&sort=startTime:DESC&freeText=*&query=
     */
    @Override
    public SearchResult<String> searchWorkflows(
            String query, String freeText, int start, int count, List<String> sort) {
    	String[] searchList = {"correlationId", "workflowId", "workflowType", "startTime", "status"};
    	String[] replaceList = {"correlation_id", "workflow_Id", "workflow_type", "start_time", "status"};
    	query = StringUtils.replaceEach(query, searchList, replaceList);
    	
    	String condition = this.handleCondition(query,freeText);
    	
    	String orderStr = this.handleSort(sort);
    	
    	String sql = "select workflow_id from index_workflow " 
    			+ condition
    			+ orderStr;
    	sql = dialect.pagin(start+1, count, sql);
    	LOGGER.debug(sql);
    	List<String> results = jdbcTemplate.queryForList(sql, String.class);
    	
    	String countSql = "select count(workflow_id) from index_workflow " 
    			+ condition;
    	Integer allCount = jdbcTemplate.queryForObject(countSql, Integer.class);
    	SearchResult<String> searchResult = new SearchResult<>(allCount, results);
        return searchResult;
    }
    
    private String handleSort(List<String> sort) {
    	String orderStr = CollectionUtils.isEmpty(sort) ? "":" order by ";
    	for (String item : sort) {
    		String[] itemA = item.split(":");
    		String colName = itemA[0];
			colName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, colName);
			orderStr = orderStr + colName + " " + (itemA.length > 1? itemA[1]:"") + ",";
		}
    	return StringUtils.substringBeforeLast(orderStr, ",");
    }
    
    
    private String handleCondition(String query,String freeText) {
    	query = this.handleValueQuot(query);
    	
    	query = StringUtils.equals("*", query) ? "":query;
    	
    	if(StringUtils.isNotBlank(freeText) && !StringUtils.equals("*", freeText)) {
    		query = query + (StringUtils.isBlank(query)?"":" AND ");
    		query = query + "json_data like '%" + freeText + "%'";
    	}
    	query = StringUtils.isBlank(query)?"":  "where " + query;
    	return query;
    }
    
    private String handleValueQuot(String query) {
    	query = StringUtils.trimToEmpty(query);
    	if("".equals(query)) {
    		return query;
    	}
    	query = StringUtils.replaceAll(query, " and ", " AND ");
    	String[] conditions = StringUtils.splitByWholeSeparator(query, " AND ");
    	for (int i = 0; i < conditions.length; i++) {
    		String condition = conditions[i];
			if(StringUtils.containsIgnoreCase(condition, " in ")) {
				String[] cArr = condition.split(" IN ");
				cArr[1] = this.wrapQuot(cArr[1]);
				condition = cArr[0] + " IN " + cArr[1];
			} else {
				condition = StringUtils.replaceAll(condition, "\"", "'");
			}
			conditions[i] = condition;
		}
    	return StringUtils.join(conditions, " AND ");
    }
    
    private String wrapQuot(String text) {
    	text = StringUtils.trimToEmpty(text);
    	if("".equals(text)) {
    		return "''";
    	}
    	boolean containsBrackets = false;
    	if(text.startsWith("(")) {
    		containsBrackets = true;
    		text = StringUtils.substringAfter(text, "(");
    		text = StringUtils.substringBeforeLast(text, ")");
    	}
		String[] textArr = text.split(",");
		for (int i = 0; i < textArr.length; i++) {
			textArr[i] = "'" +textArr[i] + "'";
		}
		text = StringUtils.join(textArr,",");
		if(containsBrackets) {
			text = "(" + text + ")";
		}
    	
    	return text;
    }

    /**
     * use sort options as sort=<field>:ASC|DESC e.g. sort=name&sort=workflowId:DESC. If order is not specified, defaults to ASC.
     */
    @Override
    public SearchResult<WorkflowSummary> searchWorkflowSummary(
            String query, String freeText, int start, int count, List<String> sort) {
    	SearchResult<String> searchResult = this.searchWorkflows(query, freeText, start, count, sort);
    	boolean includeTasks = false;
    	List<WorkflowSummary> workflows = searchResult.getResults().stream()
		        .parallel()
		        .map(
		                workflowId -> {
		                    try {
		                        WorkflowModel workflowModel = executionDAO.getWorkflow(workflowId, includeTasks);
		                        return workflowModel == null ? null : new WorkflowSummary(workflowModel.toWorkflow());
		                    } catch (NotFoundException e) {
		                        // This might happen when the workflow archival failed and the
		                        // workflow was removed from primary datastore
		                        LOGGER.error(
		                                "Error getting the workflow  for query: {} and freeText: {}  from index",
		                                query,
		                                freeText,
		                                e);
		                        return null;
		                    }
		                })
		        .filter(Objects::nonNull)
		        .collect(Collectors.toList());
        return new SearchResult<WorkflowSummary>(searchResult.getTotalHits(), workflows);
    }

    /**
     * taskDefName IN (task_1) AND taskType IN (DO_WHILE) AND taskId="23" AND status IN (CANCELED) AND updateTime>1671070920000 AND updateTime<1671070920000 AND workflowType IN (test_groovy)
     */
    @Override
    public SearchResult<String> searchTasks(
            String query, String freeText, int start, int count, List<String> sort) {
    	
    	String[] searchList = {"correlationId", "workflowId", "workflowType", "startTime", "updateTime", "status", "taskDefName", "taskType", "taskId"};
    	String[] replaceList = {"correlation_id", "workflow_Id", "workflow_type", "start_time", "update_time", "status", "task_def_name", "task_type", "task_id"};
    	query = StringUtils.replaceEach(query, searchList, replaceList);

    	String condition = this.handleCondition(query,freeText);
    	String orderStr = this.handleSort(sort);
    	
    	String sql = "select task_id from index_task " 
    			+ condition
    			+ orderStr;
    	sql = dialect.pagin(start + 1, count, sql);
    	LOGGER.debug(sql);
    	List<String> results = jdbcTemplate.queryForList(sql, String.class);
    	
    	String countSql = "select count(task_id) from index_task " 
    			+ condition;
    	Integer allCount = jdbcTemplate.queryForObject(countSql, Integer.class);
    	SearchResult<String> searchResult = new SearchResult<>(allCount, results);
        return searchResult;
    }

    @Override
    public SearchResult<TaskSummary> searchTaskSummary(
            String query, String freeText, int start, int count, List<String> sort) {
    	SearchResult<String> searchResult = this.searchTasks(query, freeText, start, count, sort);
    	List<TaskSummary> workflows = searchResult.getResults().stream()
		        .parallel()
		        .map(
		                taskId -> {
		                    try {
		                        TaskModel taskModel = executionDAO.getTask(taskId);
		                        return taskModel == null ? null : new TaskSummary(taskModel.toTask());
		                    } catch (NotFoundException e) {
		                        // This might happen when the workflow archival failed and the
		                        // workflow was removed from primary datastore
		                        LOGGER.error(
		                                "Error getting the workflow  for query: {} and freeText: {}  from index",
		                                query,
		                                freeText,
		                                e);
		                        return null;
		                    }
		                })
		        .filter(Objects::nonNull)
		        .collect(Collectors.toList());
        return new SearchResult<TaskSummary>(searchResult.getTotalHits(), workflows);
    }

    @Override
    public void removeWorkflow(String workflowId) {
    	this.removeWorkflowTask(workflowId);
    	this.removeWorkflowNotIncludeTask(workflowId);
    }
    
    public void removeWorkflowNotIncludeTask(String workflowId) {
    	jdbcTemplate.update("delete from index_workflow where workflow_id = ?", workflowId);
    }
    
    private void removeWorkflowTask(String workflowId) {
    	jdbcTemplate.update("delete from index_task where workflow_id = ?", workflowId);
    }

    @Override
    public CompletableFuture<Void> asyncRemoveWorkflow(String workflowId) {
        return CompletableFuture.runAsync(() -> removeWorkflow(workflowId), executorService);
    }

    @Override
    public void updateWorkflow(String workflowInstanceId, String[] keys, Object[] values) {
    	//该方法目前只在ExecutionDAOFacade.removeWorkflowIndex中调用，归档开启时会被调用，由于暂时不支持归档，则删除时直接删除
    	if(keys.length > 1 && ARCHIVED_FIELD.equals(keys[1])) {
    		this.removeWorkflow(workflowInstanceId);
    	}
    }

    @Override
    public CompletableFuture<Void> asyncUpdateWorkflow(
            String workflowInstanceId, String[] keys, Object[] values) {
        return CompletableFuture.runAsync(
                () -> updateWorkflow(workflowInstanceId, keys, values), executorService);
    }

    @Override
    public String get(String workflowInstanceId, String fieldToGet) {
    	//暂不实现
        return null;
    }

    @Override
    public List<String> searchArchivableWorkflows(String indexName, long archiveTtlDays) {
    	//暂不实现
        return null;
    }

    @Override
    public long getWorkflowCount(String query, String freeText) {
    	//暂不实现
        return 0;
    }

    @Override
    public void setup() throws Exception {
    }
}
