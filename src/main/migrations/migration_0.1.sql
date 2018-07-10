create table authorized_user (
  user_id bigserial primary key,
  chat_id bigint not null
);

create index xi_authorized_user_chat_id on authorized_user(chat_id);

create table text_to_replace (
  key_text text not null primary key,
  value_text text not null,
  creator_id bigint not null references authorized_user(user_id),
  creation_date timestamp not null,
  modifier_id bigint not null references authorized_user(user_id),
  modification_date timestamp not null
);

