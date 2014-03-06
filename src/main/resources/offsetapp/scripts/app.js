var app = angular.module('offsetapp',
						 ["offsetapp.controllers", "offsetapp.directives",  "ngRoute"],
                                                 function($routeProvider) {
                                                         $routeProvider
														 .when("/", {
															 templateUrl: "views/grouplist.html",
															 controller: "GroupListCtrl"
														 })
                                                         .when("/group/:group", {
                                                             templateUrl: "views/group.html",
                                                             controller: "GroupCtrl"
                                                         })
                                                         .when("/group/:group/:topic", {
                                                             templateUrl: "views/topic.html",
                                                             controller: "TopicCtrl"
                                                         });;
                                                 });

angular.module("offsetapp.services", ["ngResource"])
	.factory("offsetinfo", ["$resource", "$http", function($resource, $http) {
		function groupPartitions(cb) {
			return function(data) {
				var groups = _(data.offsets).groupBy(function(p) {
					var t = p.timestamp;
					if(!t) t = 0;
					return p.group+p.topic+t.toString();
				});
				groups = groups.values().map(function(partitions) {
					return {
						group: partitions[0].group,
						topic: partitions[0].topic,
						partitions: partitions,
						logSize: _(partitions).pluck("logSize").reduce(function(sum, num) {
							return sum + num;
						}),
						offset: _(partitions).pluck("offset").reduce(function(sum, num) {
							return sum + num;
						}),
						timestamp: partitions[0].timestamp
					};
				}).value();
				data.offsets = groups;
				cb(data);
			};
		}

		return {
			getGroup: function(group, cb) {
				return $resource("./group/:group").get({group:group}, groupPartitions(cb));
			},
			listGroup: function() {return $http.get("./group");},
			getTopic: function(group, topic, cb) {
				return $resource("./group/:group/:topic").get({group:group, topic: topic}, groupPartitions(cb));
			}
		};
	}]);
