package com.beepsoft.hasuraconf.model

import com.beepsoft.hasuraconf.annotation.AllowAggregationsEnum
import com.beepsoft.hasuraconf.annotation.HasuraOperation
import com.beepsoft.hasuraconf.annotation.HasuraPermission
import com.beepsoft.hasuraconf.annotation.HasuraPermissions
import javax.persistence.*

@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
// Testing permutated permission definitions, ie. a single HasuraPermission causing the generation of multiple
// permission definitions
@HasuraPermissions([
     HasuraPermission(
         operation = HasuraOperation.ALL,
         roles = ["AUTHOR", "EDITOR"],
         json="""
         {
           id: {_gt:10}
         }
        """
     ),
    HasuraPermission(
        operations = [HasuraOperation.SELECT, HasuraOperation.UPDATE],
        roles = ["WORKER", "BOSS"],
        json="""
             {
                "name": {
                    "_like": "%some_value%"
                }
             }
         """,
        allowAggregations = AllowAggregationsEnum.TRUE
    )
])
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
    @HasuraPermissions([
        HasuraPermission(
            operation = HasuraOperation.SELECT,
            role="USER",
            fields = ["operationId", "taskId"],
            json = """
                {
                    "task": {
                        "owner": {
                            "id": {
                                "_eq": "x-hasura-user-id"
                            }
                        }
                    }
                }
            """
        )
    ])
    private val tasks: Set<Task>? = null

    private val name: String? = null
    private val description: String? = null
}
