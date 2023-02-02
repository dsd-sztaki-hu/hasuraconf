package com.beepsoft.hasuraconf.model3

import com.beepsoft.hasuraconf.annotation.HasuraCustomRootFields
import com.beepsoft.hasuraconf.annotation.HasuraNamingConvention
import com.beepsoft.hasuraconf.annotation.HasuraSourceCustomization
import com.beepsoft.hasuraconf.annotation.HasuraSourceTypeCustomization
import com.beepsoft.hasuraconf.model.BaseObject
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table

@Entity
@Table(
    indexes = arrayOf(
        Index(columnList = "id")
    )
)
@HasuraSourceCustomization(
    rootFields = HasuraCustomRootFields(
        select = "doComment2Select",
        selectByPk = "doComment2SelectOne",
        insert = "doComment2Insert",
        insertOne = "doComment2InsertOne",
        update = "doComment2Update",
        updateByPk = "doComment2UpdateOne",
        delete = "doDComment2elete",
        deleteByPk = "doComment2DeleteOne",
    ),

    namingConvention = HasuraNamingConvention.GRAPHQL_DEFAULT

)
class Comment2 : BaseObject() {
}
