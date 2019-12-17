BEGIN;
INSERT INTO public.user_role_type (value, description) VALUES ('ROLE_USER', 'A normal user"') ON CONFLICT DO NOTHING;
INSERT INTO public.user_role_type (value, description) VALUES ('ROLE_ORGANIZER', 'An organizer') ON CONFLICT DO NOTHING;
INSERT INTO public.user_role_type (value, description) VALUES ('ROLE_ADMIN', 'An admin user') ON CONFLICT DO NOTHING;
END;
