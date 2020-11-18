package com.beepsoft.hasuraconf.model

import javax.persistence.*

@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
class Operation : BaseObject() {
    /**
     * Task the operation belongs to.
     */
    @ManyToMany(cascade = [CascadeType.ALL])
    @OrderColumn(name = "order_in_task", nullable = false)
    @JoinTable(name = "task_operation",
            joinColumns = [JoinColumn(name = "operation_id")],
            inverseJoinColumns = [JoinColumn(name = "task_id")],
            foreignKey = ForeignKey(value = ConstraintMode.CONSTRAINT, name = "operation_task_operation_id_fkey", foreignKeyDefinition = "FOREIGN KEY (operation_id) references operation (id) ON DELETE CASCADE"),
            inverseForeignKey = ForeignKey(value = ConstraintMode.CONSTRAINT, name = "operation_task_task_id_fkey", foreignKeyDefinition = "FOREIGN KEY (task_id) references task (id) ON DELETE CASCADE"))
    private val tasks: Set<Task>? = null
}