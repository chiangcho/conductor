CREATE TABLE index_workflow_task (
    task_id VARCHAR2(255) NOT NULL,
    workflow_id VARCHAR2(255) NOT NULL,
    correlation_id VARCHAR2(255) DEFAULT NULL,
    workflow_type VARCHAR2(255) DEFAULT NULL,
    workflow_priority NUMBER(5) DEFAULT NULL,
    task_def_name VARCHAR2(255) DEFAULT NULL,
    task_type VARCHAR2(255) DEFAULT NULL,
    domain VARCHAR2(255) DEFAULT NULL,
    execution_time NUMBER(11) DEFAULT NULL,
    queue_wait_time NUMBER(11) DEFAULT NULL,
    reason_for_incompletion CLOB,
    status VARCHAR2(50) DEFAULT NULL,
    json_data CLOB,
    scheduled_time TIMESTAMP DEFAULT NULL,
    start_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT index_workflow_task_pk PRIMARY KEY (task_id),
    CONSTRAINT index_workflow_task_corr_id_idx FOREIGN KEY (correlation_id) REFERENCES index_workflow(correlation_id)
);

CREATE TABLE index_workflow_task_execution_log (
    task_execution_id NUMBER(11) NOT NULL,
    task_id VARCHAR2(255) NOT NULL,
    log CLOB,
    created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT index_workflow_task_execution_log_pk PRIMARY KEY (task_execution_id)
);

CREATE TABLE index_workflow (
    workflow_id VARCHAR2(255) NOT NULL,
    correlation_id VARCHAR2(255) DEFAULT NULL,
    workflow_type VARCHAR2(255) DEFAULT NULL,
    workflow_version NUMBER(5) DEFAULT NULL,
    reason_for_incompletion CLOB,
    status VARCHAR2(50) DEFAULT NULL,
    json_data CLOB,
    execution_time NUMBER(11) DEFAULT NULL,
    start_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    end_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT index_workflow_pk PRIMARY KEY (workflow_id),
    CONSTRAINT index_workflow_corr_id_idx INDEX (correlation_id)
);
