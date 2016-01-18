namespace java QPMstubs

service QPM
{
	# QPM <--> client API
	string handleQuery(1:string query),

	# simple function to test connections
	void ping()
}
