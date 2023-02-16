CREATE TABLE index_workflow_task (
    task_id VARCHAR(255) NOT NULL,
    workflow_id VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255),
    workflow_type VARCHAR(255),
    workflow_priority SMALLINT,
    task_def_name VARCHAR(255),
    task_type VARCHAR(255),
    domain VARCHAR(255),
    execution_time INTEGER,
    queue_wait_time INTEGER,
    reason_for_incompletion CLOB(5M),
    status VARCHAR(50),
    json_data CLOB(10M),
    scheduled_time TIMESTAMP,
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP,
    end_time TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP,
    PRIMARY KEY (task_id)
);

CREATE TABLE index_workflow_task_execution_log (
    task_execution_id INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    task_id VARCHAR(255) NOT NULL,
    log CLOB(10M) NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP,
    PRIMARY KEY (task_execution_id)
);


CREATE TABLE index_workflow (
    workflow_id VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255) NULL,
    workflow_type VARCHAR(255) NULL,
    workflow_version SMALLINT NULL,
    reason_for_incompletion CLOB(10m),
    status VARCHAR(50) NULL,
    json_data CLOB(10m),
    execution_time INTEGER,
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP,
    end_time TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP,
    PRIMARY KEY (workflow_id)
);
