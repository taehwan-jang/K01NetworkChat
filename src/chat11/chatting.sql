create table chat_talking (
    idx number primary key,
    chatRoom varchar2(30) not null,
    sendN varchar2(20),
    receiveN varchar(20) default 'All',
    contents varchar2(200) not null,
    idate date default sysdate
);
drop table chat_talking;
drop sequence seq_talking;
create sequence seq_talking 
    increment by 1
    start with 1
    minvalue 1
    nomaxvalue
    nocycle
    nocache;
    
select * from chat_talking;

insert into chat_talking (idx,sendN,contents,idate) values(seq_talking.nextval,'장태환','안녕하세요',sysdate);