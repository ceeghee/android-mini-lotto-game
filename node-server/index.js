
var TIME_LIMIT = 10;
var SLEEP_TIME = 10;

function User(id,money,betValue){
	this.id = id // Keep connection id(Socket id)
	this.money = money //keep user money
	this.betValue = betValue // key user bet value

}

var ArrayList = require('arraylist')

var app = require('express')();
var http = require('http').Server(app);
var io = require("socket.io")(http);

//Create a List of User Playing
var listUsers	= new ArrayList;

var money		= 0; //Total money bet on server

//create server
app.get('/',function(req,res){
	res.sendFile('index.html',{root:__dirname}); // When Get Method is used, return default Index
})

function getRandomInt(max){
	return Math.floor(Math.random()*Math.floor(max)); // Create random int from 0 to max
}

function sleep(sec){
	return new Promise(resolve => setTimeout(resolve,sec*1000)) //sleep
}

async function countDown(){
	var timeTotal = TIME_LIMIT;

	do{
		//send timer to all clients
		io.sockets.emit('broadcast',timeTotal);
		timeTotal--;
		await sleep(1); // sleep for 1 sec
	}while(timeTotal > 0)

	//After time limit is finish

	processResult(); // Send reward money for winner

	//Reset data for next turn
	timerTotal = TIME_LIMIT;
	money = 0;
	io.sockets.emit("wait_before_restart", SLEEP_TIME); // Send Message to wait, before server calculate result before next turn
	io.sockets.emit('money_send',0); //Send total of money to all user (next turn default is 0)
	await sleep(SLEEP_TIME); //Wait


	io.sockets.emit('restart',1); // Send message next turn for all client (aany number can be used)
	countDown();
}

function processResult(){
	console.log("Server is Processing Data");
	var result 	= getRandomInt(2); // Generate from 0-1
	console.log('Lucky Number : '+ result);
	io.sockets.emit('result',result); // Send lucky number to all client

	listUsers.unique();

	//count in list user playing and how many winners
	var count	= listUsers.find(function(user){
		return user.betValue == result;
	}).length;

	//Now lets find winner and loser to send reward
	listUsers.find(function(user){
		if(user.betValue == result)
			io.to(user.id).emit("reward", parseInt(user.money)*2); // Double betMoney for won user
		else
			io.to(user.id).emit('lose',user.money);
	});

	console.log('We Have '+count+' people(s) Winners');

	//clear list players
	listUsers.clear();
}

//process connection socket
io.on('connection',function(socket){
	console.log("A new user "+socket.id+" is connected")

	io.sockets.emit('money_send',money); //As soon as user logged on Server, send sum of money of this turn to him

	socket.on('client_send_money',function(oobjectClient){
		//when user place aa bet, we will get money and increase our total money
		console.log(oobjectClient)
		var user = new User(socket.id, oobjectClient.money, oobjectClient.betValue);

		console.log("we receive : "+user.money+' from '+user.id);
		console.log("Users : "+user.id+' bet value '+user.betValue);

		money+=parseInt(user.money)

		console.log("Sum of Money : "+money) // Update on our server

		//save user to list user online
		listUsers.add(user);
		console.log("Total online users : "+listUsers.length);

		//send update money to all user
		io.sockets.emit("money_send",money);
	})
		socket.on('disconnect',function(socket){
		console.log("User "+ socket.id +" left");
		})
})

//starrt server
http.listen(3000,function(){
	console.log("Server Started on Port : 3000");

	countDown();
})

