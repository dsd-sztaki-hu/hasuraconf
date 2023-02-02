package com.beepsoft.hasuraconf.model2

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
        select = "doSelect",
        selectByPk = "doSelectOne",
        selectAggregate = "doSelectAggregate",
        insert = "doInsert",
        insertOne = "doInsertOne",
        update = "doUpdate",
        updateByPk = "doUpdateOne",
        delete = "doDelete",
        deleteByPk = "doDeleteOne",
    ),

    typeName = HasuraSourceTypeCustomization(
        prefix = "TypePrefix",
        suffix = "TypeSuffix"
    ),

    namingConvention = HasuraNamingConvention.HASURA_DEFAULT

)
class Comment1 : BaseObject() {
}
