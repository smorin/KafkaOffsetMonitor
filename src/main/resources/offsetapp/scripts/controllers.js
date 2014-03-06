angular.module('offsetapp.controllers',["offsetapp.services"])
	.controller("GroupCtrl", ["$scope", "$interval", "$routeParams", "offsetinfo",
							  function($scope, $interval, $routeParams, offsetinfo) {
								  offsetinfo.getGroup($routeParams.group, function(d) {
									  $scope.info = d;
									  $scope.loading=false;
								  });
								  $scope.loading=true;

								  $scope.group = $routeParams.group;
							  }])
	.controller("GroupListCtrl", ["$scope", "offsetinfo",
								  function($scope, offsetinfo) {
									  $scope.loading = true;
									  offsetinfo.listGroup().success(function(d) {
										  $scope.loading=false;
										  $scope.groups = d;
									  });
								  }])
	.controller("TopicCtrl", ["$scope", "$routeParams", "offsetinfo",
							  function($scope, $routeParams, offsetinfo) {
								  $scope.group = $routeParams.group;
								  $scope.topic = $routeParams.topic;
								  $scope.data = [];
								  offsetinfo.getTopic($routeParams.group, $routeParams.topic, function(d) {
									  $scope.data = d.offsets;
								  });
							  }]);
