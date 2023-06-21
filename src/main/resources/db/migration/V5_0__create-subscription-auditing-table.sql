CREATE TABLE public.subscription_auditing (
    id character varying NOT NULL,
    "queueName" character varying NOT NULL,
    "eventType" public.event_type NOT NULL,
    "subscriptionType" public.sub_type NOT NULL,
    "userId" uuid NOT NULL,
    resource character varying
);

ALTER TABLE public.subscription_auditing OWNER TO iudx_rs_user;

ALTER TABLE ONLY public.subscription_auditing
    ADD CONSTRAINT subscription_auditing_pkey PRIMARY KEY (id);

