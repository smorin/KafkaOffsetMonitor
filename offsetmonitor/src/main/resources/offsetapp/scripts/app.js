var app = angular.module('offsetapp', 
						 ["offsetapp.controllers", "offsetapp.directives",  "ngRoute"],
                                                 function($routeProvider) {
                                                         $routeProvider
														 .when("/", {
															 templateUrl: "views/root.html",
															 controller: "RootCtrl"
														 })
                                                         .when("/group/:group", {
                                                             templateUrl: "views/main.html",
                                                             controller: "MainCtrl"
                                                         })
                                                         .when("/group/:group/:topic", {
                                                             templateUrl: "views/topic.html",
                                                             controller: "TopicCtrl"
                                                         });;
                                                 });

angular.module("offsetapp.services", ["ngResource"])
	.factory("offsetinfo", ["$resource", "$http", function($resource, $http) {
		return {
			get: function(group, cb) {
				return $resource("./group/:group").get({group:group}, cb);
			},
			list: function() {return $http.get("./group");}
		};
	}])
	.factory("datapoints", function() {
		var data = {};
		return {
			addPoint: function(group, topic, logSize, offset) {
				if(!data[group]) data[group] = {};
				if(!data[group][topic]) data[group][topic] = [];
				data[group][topic].push({
					timestamp: Date.now(),
					logSize: logSize,
					offset: offset
				});
			},
			getPoints: function(group, topic) {
				if(!data[group]) return [];
				if(!data[group][topic]) return [];
				return data[group][topic];
			},
			getAll: function() {
				return data;
			}
		};
	});
