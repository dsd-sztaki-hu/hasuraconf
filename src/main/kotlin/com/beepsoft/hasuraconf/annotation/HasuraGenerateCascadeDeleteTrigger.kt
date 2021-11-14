package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Generates trigger to delete the marked field when the owning record is deleted.
 *
 *
 * Postgresql's ON DELETE CASCADE only works from child to parent, ie. iIf there's a child referencing a parent and the
 * parent is deleted, then the child will be deleted as well.
 * it works in this case
 * <pre>
 * @OneToMany(mappedBy = "task")
 * @OnDelete(action= OnDeleteAction.CASCADE)
 * List<Operation> operations;
</Operation></pre> *
 * But not in case of:
 * <pre>
 * @OneToOne(optional = false)
 * @JoinColumn(nullable=false)
 * @OnDelete(action= OnDeleteAction.CASCADE)
 * private Text label;
</pre> *
 * In the later case if Text gets deleted it would delete the Task referencing it. However we want it the other way
 * around: we want to delete the Text if teh Task gets deleted. For this we will need a trigger that handles this
 * and we can use HasuraGenerateCascadeDeleteTrigger for this:
 * <pre>
 * @OneToOne(optional = false)
 * @JoinColumn(nullable=false)
 * @HasuraCascadeDeleteTrigger
 * private Text label;
</pre> *
 *
 * This will generate into init.json a trigger like this:
 * <pre>
 * DROP TRIGGER IF EXISTS task_text_cascade_delete_trigger ON task;;
 * DROP FUNCTION  IF EXISTS task_text_cascade_delete();
 * CREATE FUNCTION task_text_cascade_delete() RETURNS trigger AS
 * $body$
 * BEGIN
 * IF TG_WHEN <> 'BEFORE' OR TG_OP <> 'DELETE' THEN
 * RAISE EXCEPTION 'task_text_cascade_delete may only run as a BEFORE DELETE trigger';
 * END IF;
 *
 * DELETE text where id=OLD.label_id
 * END;
 * $body$
 * LANGUAGE plpgsql;;
 * CREATE TRIGGER task_text_cascade_delete_trigger BEFORE DELETE ON task
 * FOR EACH ROW EXECUTE PROCEDURE task_text_cascade_delete();;
</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraGenerateCascadeDeleteTrigger
