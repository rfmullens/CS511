-module(server).

-export([start_server/0]).

-include_lib("./defs.hrl").

-spec start_server() -> _.
-spec loop(_State) -> _.
-spec do_join(_ChatName, _ClientPID, _Ref, _State) -> _.
-spec do_leave(_ChatName, _ClientPID, _Ref, _State) -> _.
-spec do_new_nick(_State, _Ref, _ClientPID, _NewNick) -> _.
-spec do_client_quit(_State, _Ref, _ClientPID) -> _NewState.

start_server() ->
    catch(unregister(server)),
    register(server, self()),
    case whereis(testsuite) of
	undefined -> ok;
	TestSuitePID -> TestSuitePID!{server_up, self()}
    end,
    loop(
      #serv_st{
	 nicks = maps:new(), %% nickname map. client_pid => "nickname"
	 registrations = maps:new(), %% registration map. "chat_name" => [client_pids]
	 chatrooms = maps:new() %% chatroom map. "chat_name" => chat_pid
	}
     ).

loop(State) ->
    receive 
	%% initial connection
	{ClientPID, connect, ClientNick} ->
	    NewState =
		#serv_st{
		   nicks = maps:put(ClientPID, ClientNick, State#serv_st.nicks),
		   registrations = State#serv_st.registrations,
		   chatrooms = State#serv_st.chatrooms
		  },
	    loop(NewState);
	%% client requests to join a chat
	{ClientPID, Ref, join, ChatName} ->
	    NewState = do_join(ChatName, ClientPID, Ref, State),
	    loop(NewState);
	%% client requests to join a chat
	{ClientPID, Ref, leave, ChatName} ->
	    NewState = do_leave(ChatName, ClientPID, Ref, State),
	    loop(NewState);
	%% client requests to register a new nickname
	{ClientPID, Ref, nick, NewNick} ->
	    NewState = do_new_nick(State, Ref, ClientPID, NewNick),
	    loop(NewState);
	%% client requests to quit
	{ClientPID, Ref, quit} ->
	    NewState = do_client_quit(State, Ref, ClientPID),
	    loop(NewState);
	{TEST_PID, get_state} ->
	    TEST_PID!{get_state, State},
	    loop(State)
    end.

%% executes join protocol from server perspective
do_join(ChatName, ClientPID, Ref, State) ->
    case maps:find(ChatName, State#serv_st.chatrooms) of
		{ok, ChatPID} ->
			ok;
		error ->
			ChatPID = spawn(chatroom, start_chatroom, [ChatName])
		end,
		Nick = maps:get(ClientPID, State#serv_st.nicks),
		ChatPID ! {self(), Ref, register, ClientPID, Nick},
		NewState = #serv_st{
						nicks = State#serv_st.nicks,
						registrations = maps:put(ChatName, [ClientPID]++maps:get(ChatName, State#serv_st.registrations, []), State#serv_st.registrations),
						chatrooms = maps:put(ChatName, ChatPID, State#serv_st.chatrooms)
						},
    NewState.

%% executes leave protocol from server perspective
do_leave(ChatName, ClientPID, Ref, State) ->
    ChatPID = maps:get(ChatName, State#serv_st.chatrooms),
		NewState = #serv_st{
				nicks = State#serv_st.nicks,
				registrations = maps:update(ChatName, ClientPID, State#serv_st.registrations),
				chatrooms = State#serv_st.chatrooms
			},
		ChatPID!{self(), Ref, unregister, ClientPID},
		ClientPID!{self(), Ref, ack_leave},
	NewState.


%% executes new nickname protocol from server perspective
do_new_nick(State, Ref, ClientPID, NewNick) ->
    case lists:member(NewNick, maps:values(State#serv_st.nicks)) of
		false ->
			NewState = #serv_st{
								nicks = maps:update(ClientPID, NewNick, State#serv_st.nicks),
								registrations = State#serv_st.registrations,
								chatrooms = State#serv_st.chatrooms
							   },
			maps:map(fun (K,V) -> case lists:member(ClientPID, V) of 
									  true ->
										  maps:get(K,State#serv_st.chatrooms)!{self(), Ref, update_nick, ClientPID, NewNick};
									  false ->
										  not_in_chat
								  end 
					 end, State#serv_st.registrations),
			ClientPID!{self(), Ref, ok_nick};
		true ->
			ClientPID!{self(), Ref, err_nick_used},
			NewState=State
	end,
	NewState.

%% executes client quit protocol from server perspective
do_client_quit(State, Ref, ClientPID) ->
    NickMap = maps:remove(ClientPID, State#serv_st.nicks),
	Registrations_New = maps:map(fun(I, J) when is_list(I) -> lists:delete(ClientPID, J) end, State#serv_st.registrations),
	ClientPID!{self(), Ref, ack_quit},
	State#serv_st{nicks = NickMap, registrations = Registrations_New}.
