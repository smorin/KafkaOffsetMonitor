angular.module('offsetapp.controllers',["offsetapp.services"])
	.controller("MainCtrl", ["$scope", "$interval", "$routeParams", "offsetinfo", "datapoints",
							 function($scope, $interval, $routeParams, offsetinfo, datapoints) {
								 function getData() {
									 offsetinfo.get($routeParams.group, function(d) {
										 $scope.info = d;
										 _.forEach(d.offsets, function(p) {
											 datapoints.addPoint(p.group, p.topic, p.logSize, p.offset);
											 p.data = datapoints.getPoints(p.group,p.topic);
										 });
									 });
								 }
								 getData();
								 var refresh = $interval(getData, 10000);

								 $scope.showing = "";

								 $scope.group = $routeParams.group;

								 $scope.$on('$destroy', function() {
									 if(angular.isDefined(refresh)) $interval.cancel(refresh);
								 });

								 $scope.toggle = function(label) {
									 if($scope.showing == label) $scope.showing="";
									 else $scope.showing = label;
								 };
							 }])
	.controller("RootCtrl", ["$scope", "offsetinfo",
							 function($scope, offsetinfo) {
								 offsetinfo.list().success(function(d) {
									  $scope.groups = d;
								 });
							 }]);
