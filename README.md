# transfer-app-scala

Transfer app implements an in-memory payment/account-management system.
It allows to create accounts, refill or withdraw funds in currencies and perform money <b>transfer</b> from one account to another.<p/>
All account operations implemented with Akka Actors: each account is backed by actor that receives operations (like refills or locks) via it's dedicated mailbox and execute sequentially in thread-safe manner.<p/>
Actual transfer operation implemented in three steps: amount lock on source account, target account refill and final lock confirmation or amount unlock if refill operation was failed.
At this point in time it looks like an overkill for me... but that was an idea :)<p/>
Application organised as an SBT project. You can run actual application with `sbt run` command.<p/> 
All operations exposed as http-api. Api usage examples:<p/>
<b>New account creation</b>: <b>PUT</b> on `/account` with json body: `{"number": "123456", "ccy": "USD"}`<p/>  
<b>Money refill</b>: <b>POST</b> on `/do/refill` with json body: `{
                                                                      "account": {"number": "123456", "ccy": "USD"},
                                                                      "amount": 100,
                                                                      "comment": "refill..."
                                                                    }`<p/>
<b>Money withdrawal</b>: <b>POST</b> on `/do/withdrawal` with json body: `{
                                                                      "account": {"number": "123456", "ccy": "USD"},
                                                                      "amount": 100
                                                                    }`<p/>
<b>Money transfer</b>: <b>POST</b> on `/do/transfer` with json body: `{
                                                                        "source": {"number": "123456", "ccy": "USD"},
                                                                        "destination": {"number": "99123456", "ccy": "USD"},
                                                                        "amount": 10,
                                                                        "comment": "donation"
                                                                      }`<p/>
<b>Account balance</b>: <b>GET</b> on `/account/total?ccy=USD&number=123456`