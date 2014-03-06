'use strict';

angular.module("offsetapp.directives", [])
	.directive('chart', function() {
		return {
			restrict: 'E',
			template: '<div>'
				+ '<div class="row speed" ng-hide="loading">'
				+ '<div class="label label-info col-md-3">'
				+ '<span class="glyphicon glyphicon-log-in"></span> '
				+ '<strong>{{inspeed|number:3}}</strong> <span class="small">msg/s</span>'
				+ '<br><small>in the last {{deltaT_min|number:0}} minutes.</small>'
				+ '</div>'
				+ '<div class="label label-info  col-md-3 col-md-offset-5">'
				+ '<strong>{{outspeed|number:3}}</strong> <span class="small">msg/s</span>'
				+ '<span class="glyphicon glyphicon-log-out"></span> '
				+ '<br><small>in the last {{deltaT_min|number:0}} minutes.</small>'
				+ '</div>'
				+ '</div>'
				+ '<div class="row">'
				+ '<div class="chart ng-md-12" ng-hide="loading"></div>'
				+ '<div class="alert ng-md-12 alert-info" ng-show="loading">Loading</div>'
				+ '</div>'
				+ '</div>',
			replace: true,
			scope: {
				data: "="
			},

			link: function (scope, element, attrs) {
				var chart = undefined;

				function setupChart(data, element) {
					var d = _(data).map(function(p) {
						return [
							[p.timestamp, p.logSize],
							[p.timestamp, p.offset],
							[p.timestamp, p.logSize-p.offset]
						];
					}).unzip().value();
					scope.loading = data.length <= 0;

					if(data.length > 5) {
						var last = data[data.length-1];
						var beforeLast = data[data.length-5];
						var deltaT_sec = (last.timestamp-beforeLast.timestamp)/1000;
						scope.deltaT_min = deltaT_sec/60;
						var deltaIn = last.logSize-beforeLast.logSize;
						scope.inspeed = deltaIn/deltaT_sec;
						var deltaOut = last.offset-beforeLast.offset;
						scope.outspeed = deltaOut/deltaT_sec;
					}

					Highcharts.setOptions({
						global : {
							useUTC : false
						}
					});

					// Create the chart
					chart= new Highcharts.StockChart( {
						chart : {
							backgroundColor: "#2E3338",
							plotBackgroundColor:"#3E444C",
							height: 700,
							width:1200,
							renderTo: $(element).find(".chart")[0]
						},rangeSelector: {
							inputEnabled: false
						},
						legend : {
							borderRadius: 0,
							backgroundColor:"#3E444C",
							borderColor: 'silver',
							enabled: true,
							margin: 30,
							itemMarginTop: 2,
							itemMarginBottom: 2,
							itemWidth:300,
							itemHoverStyle: {
								color: "white"
							},
							itemHiddenStyle: {
								color: "#2E3338"
							},
							itemStyle: {
								width:280,
								color: "#CCC"
							}
						},
						//				yAxis: axis,
						xAxis: {
							type: 'datetime',
							dateTimeLabelFormats: { // don't display the dummy year
								month: '%e. %b',
								year: '%b'
							},
							ordinal: false},
						yAxis: [{
							title: {
								text: "Offset Position",
								style: {
									color: '#4572A7'
								}
							},
							labels: {
								style: {
									color: '#4572A7'
								}
							},
							opposite: false
						},{
							title: {
								text: "Lag",
								style: {
									color: '#EC4143'
								}
							},
							labels: {
								style: {
									color: '#EC4143'
								}
							},
							opposite: true
						}],
						series : [{
							name: "log size",
							data:d[0],
							yAxis: 0,
							color: '#088CFE',
							marker : {
								enabled : true,
								radius : 3
							}},
								  {
									  name: "offset",
									  data:d[1],
									  color: '#B9E6D9',
									  yAxis: 0,
									  marker : {
										  enabled : true,
										  radius : 3
									  }},
								  {
									  data:d[2],
									  name: "lag",
									  color: '#EC4143',
									  yAxis: 1,
									  marker : {
										  enabled : true,
										  radius : 3
									  }}]
					});
				}

				setupChart(scope.data, element);
				//Update when charts data changes
				scope.$watch("data.length", function(newValue, oldValue) {
					if(chart != undefined)  {
						chart.destroy();
						chart = undefined;
					}
					setupChart(scope.data, element);
				});
			}

		};
	});
