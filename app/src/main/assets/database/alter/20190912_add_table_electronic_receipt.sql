create table if not exists electronic_receipt (
	_id integer primary key autoincrement,
	transaction_id integer not null,
	qr_code text not null,
	check_status integer not null default 0,
	request_status integer not null default 0,
	response_data text
);

insert into electronic_receipt (transaction_id, qr_code, check_status, request_status, response_data)
	select
		_id,
		e_receipt_qr_code,
		case when e_receipt_data like "{%" then 204 else 0 end as check_status,
		case when e_receipt_data like "{%" then 200 else 0 end as request_status,
		e_receipt_data
	from transactions where e_receipt_qr_code is not null;

update transactions set e_receipt_qr_code = null, e_receipt_data = null where e_receipt_qr_code is not null;