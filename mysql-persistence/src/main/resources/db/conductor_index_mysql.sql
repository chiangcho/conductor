CREATE TABLE `index_workflow_task` (
	`task_id` VARCHAR(255) NOT NULL COLLATE 'utf8_general_ci',
	`workflow_id` VARCHAR(255) NOT NULL COLLATE 'utf8_general_ci',
	`correlation_id` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`workflow_type` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`workflow_priority` SMALLINT(6) NULL DEFAULT NULL,
	`task_def_name` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`task_type` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`domain` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`execution_time` INT(11) NULL DEFAULT NULL,
	`queue_wait_time` INT(11) NULL DEFAULT NULL,
	`reason_for_incompletion` MEDIUMTEXT NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`status` VARCHAR(50) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`json_data` MEDIUMTEXT NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`scheduled_time` TIMESTAMP NULL DEFAULT NULL,
	`start_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`end_time` TIMESTAMP NULL DEFAULT NULL,
	`update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`task_id`) USING BTREE,
	INDEX `workflow_corr_id_index` (`correlation_id`) USING BTREE
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
ROW_FORMAT=DYNAMIC
;

CREATE TABLE `index_workflow_task_execution_log` (
	`task_execution_id` INT(11) NOT NULL AUTO_INCREMENT,
	`task_id` VARCHAR(255) NOT NULL COLLATE 'utf8_general_ci',
	`log` MEDIUMTEXT NOT NULL COLLATE 'utf8_general_ci',
	`created_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`task_execution_id`) USING BTREE
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
ROW_FORMAT=DYNAMIC
;

CREATE TABLE `index_workflow` (
	`workflow_id` VARCHAR(255) NOT NULL COLLATE 'utf8_general_ci',
	`correlation_id` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`workflow_type` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`workflow_version` SMALLINT(6) NULL DEFAULT NULL,
	`reason_for_incompletion` MEDIUMTEXT NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`status` VARCHAR(50) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`json_data` MEDIUMTEXT NULL DEFAULT NULL COLLATE 'utf8_general_ci',
	`execution_time` INT(11) NULL DEFAULT NULL,
	`start_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`end_time` TIMESTAMP NULL DEFAULT NULL,
	`update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`workflow_id`) USING BTREE,
	INDEX `workflow_corr_id_index` (`correlation_id`) USING BTREE
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
ROW_FORMAT=DYNAMIC
;
