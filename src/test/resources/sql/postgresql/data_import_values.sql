BEGIN;
INSERT INTO public.calendar_availability (value, description) VALUES ('PRIVATE', 'Only users with explicit role have read/write access to the calendar') ON CONFLICT DO NOTHING;
INSERT INTO public.calendar_availability (value, description) VALUES ('PUBLIC', 'Anyone has read access to the calendar') ON CONFLICT DO NOTHING;
INSERT INTO public.calendar_role_type (value, description) VALUES ('OWNER', 'Main owner (creator) of the calendar with admin rights"') ON CONFLICT DO NOTHING;
INSERT INTO public.calendar_role_type (value, description) VALUES ('EDITOR', 'May edit the calendar') ON CONFLICT DO NOTHING;
INSERT INTO public.calendar_role_type (value, description) VALUES ('VIEWER', 'May view the calendar') ON CONFLICT DO NOTHING;
END;
