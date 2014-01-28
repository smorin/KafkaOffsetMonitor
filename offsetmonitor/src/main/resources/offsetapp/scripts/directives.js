'use strict';

angular.module("offsetapp.directives", [])
	.directive('chart', function() {
		return {
			restrict: 'E',
			template: '<div><div class="chart"></div><div class="loading">Loading</div></div>',
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
								width:600,
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
