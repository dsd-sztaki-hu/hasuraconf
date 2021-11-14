package com.beepsoft.hasuraconf.model

import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import javax.persistence.*
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper

@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
class Task : BaseObject() {
    @ManyToOne
    var owner : CalendarUser? = null

    /**
     * Operations in the task.
     */
    @ManyToMany(cascade = [CascadeType.ALL])
    @OrderColumn(name = "order_in_task", nullable = false)
    @JoinTable(name = "task_operation", joinColumns = [JoinColumn(name = "task_id")],
            inverseJoinColumns = [JoinColumn(name = "operation_id")],
            foreignKey = ForeignKey(value = ConstraintMode.CONSTRAINT, name = "operation_task_task_id_fkey", foreignKeyDefinition = "FOREIGN KEY (task_id) references task (id) ON DELETE CASCADE"),
            inverseForeignKey = ForeignKey(value = ConstraintMode.CONSTRAINT, name = "operation_task_operation_id_fkey", foreignKeyDefinition = "FOREIGN KEY (operation_id) references operation (id) ON DELETE CASCADE"))
    var operations: List<Operation>? = null
}
