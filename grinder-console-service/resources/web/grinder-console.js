jQuery(function($) {

    function addChangeDetection(scope) {
        var changeables = $(".changeable", scope);

        if (!changeables.length) {
            return;
        }

        $("label", scope).each(function() {
            var l = this;

            if (l.htmlFor != '') {
                var e = $("#" + l.htmlFor, scope)[0];

                if (e) {
                    e.label = this;
                } else {
                    $("[name='" + l.htmlFor + "']", scope).each(function() {
                        this.label = l;
                    });
                }
            }
        });

        jQuery.fn.visible = function(show) {
            return this.css("visibility", show ? "visible" : "hidden");
        };

        var submit = $("#submit", scope);
        submit.visible(false);

        changeables.each(function() {

            if (this.type === "checkbox") {
                this.modified = function() {
                    return this.checked != this.defaultChecked;
                };
            } else {
                this.original = this.value;
                this.modified = function() {
                    return this.original != this.value;
                };
            }

            $(this).change(function(e) {
                // This is wrong if multiple controls share the same label.
                if (e.target.modified()) {
                    $(e.target.label).addClass("changed");
                } else {
                    $(e.target.label).removeClass("changed");
                }

                submit.visible(changeables.filter(function(x) {
                    return this.modified();
                }).length);
            });
        });
    }

    function pollLiveData(scope) {

        $(".live-data", scope).each(function() {
            //console.log("Registering ", this);
            var key = $(this).data("ld-key");
            var seq = $(this).data("ld-seq");

            var xhr = null;

            function poll(e) {
                //console.log("Polling ", e);
                xhr = $.get("/ui/poll", {k : key, s: seq}, function(x) {
                    //console.log("Update ", x);

                    var ee = $(e);

                    ee.trigger("livedata", [key, x]);

                    if (ee.hasClass("live-data-animation")) {
                        ee
                        .stop()
                        .animate({opacity: 0.5},
                                "fast",
                                function() {
                            $(this)
                            .html(x.data)
                            .animate({opacity: 1}, "fast");
                        });
                    }
                    else if (ee.hasClass("live-data-display")) {
                        ee.html(x.data);
                    }

                    seq = x.next;

                    // Dispatch in timer - directly calling poll()
                    // causes FF to spin sometimes.
                    setTimeout(function() {poll(e);}, 1);
                },
                "json");
            }

            var thisElement = this;

            $(document).bind("DOMNodeRemoved", function(e) {
                if (e.target == thisElement) {
                    if (xhr != null)  {
                        xhr.abort();
                    }
                }
            });

            poll(this);
        });
    }

    function addButtons(scope) {
        content = $("div#content");

        $(".grinder-button", scope).each(function() {
            if (this.id) {

                var buttonOptions;

                if (this.classList.contains("grinder-button-icon")) {
                    buttonOptions = {
                            icons: { primary: this.id }
                    };
                }
                else {
                    buttonOptions = {};
                }

                $(this).button(buttonOptions);

                if (this.classList.contains("replace-content")) {
                    $(this).click(function() {
                        $.get("/ui/content/" + this.id,
                           function(x) {
                                content.animate(
                                    {opacity: 0},
                                    "fast",
                                    function() {
                                        content.html(x);
                                        addDynamicBehaviour(content);
                                        content.animate({opacity: 1}, "fast");
                                    });
                           });
                        });
                }
                else {
                    $(this).click(function() {
                        $.post("/ui/action/" + this.id);
                    });
                }
            } else {
                $(this).button();
            };
        });
    }

    function cubismCharts() {
        if (!$("#cubism").length) {
            return;
        }

        var w = $("#cubism").width();

        var context = cubism.context()
                        .step(2000)
                        .size(w);

        // Maybe there's a neater way to do this with d3?
        $("#cubism").each(function() {
            var thisElement = this;
            $(document).bind("DOMNodeRemoved", function(e) {
                if (e.target == thisElement) {
                    context.stop();
                }
            });
        });

        d3.select("#cubism").selectAll(".axis")
            .data(["top", "bottom"])
            .enter().append("div")
            .attr("class", function(d) { return d + " axis"; })
            .each(function(d) {
                d3.select(this).call(context.axis().ticks(12).orient(d)); });

        d3.select("#cubism").append("div")
            .attr("class", "rule")
            .call(context.rule());

        var selected_statistic = 0;

        var new_data = function(data_fn) {
                // data_fn is passed an array of the existing data items and
                // returns the new data items.

                // Bind tests to nodes.
                var selection = d3.select("#cubism").selectAll(".horizon");

                var binding = selection
                    .data(function() { return data_fn(selection.data()); },
                          function(metric) { return metric.key; });

                // Handle new nodes.
                // How do we alter the sort order?
                binding.enter().insert("div", ".bottom")
                    .attr("class", "horizon")
                    .call(context.horizon()
                            .format(d3.format(",.3r"))
                            .colors(["#225EA8",
                                     "#41B6C4",
                                     "#A1DAB4",
                                     "#FFFFCC",
                                     "#FECC5C",
                                     "#FD8D3C",
                                     "#F03B20",
                                     "#BD0026"]));

                binding.exit().remove();

                context.on("focus", function(i) {
                    d3.selectAll(".value")
                    .style("right",
                            i == null ? null : context.size() - i + "px");
                });
            };

        $("#test").on('livedata', function(_e, k, x) {
            if (k === "sample") {
                new_data(
                    function(existing) {
                        var by_test= {};

                        $(existing).each(function() {
                            by_test[this.test.test] = this;
                         });

                        return $.map(
                                x.data.tests,
                                function(t) {
                                    return cubismMetric(by_test[t.test],
                                                        x.data.timestamp,
                                                        t,
                                                        selected_statistic);
                                });
                      });
            }
        });

        function cubismMetric(existing, timestamp, test, statistic) {
            var metric;

            if (existing) {
                metric = existing;
            }
            else {
                // We may want to replace this with a binary tree.
                // For now we just have an array in timestamp order.
                // Each element is an array pair of timestamp and statistic.
                // We assume that we're called with increasing timestamps.
                var stats = [];

                var average = function(ss, s) {
                        if (ss.length == 0) {
                            return NaN;
                        }

                        var total =
                            ss.reduce(function(x, y) { return x + y[s]; }, 0);

                        return total / ss.length;
                    };

                var metric_fn = function(statistic) {
                        return function(start, stop, step, callback) {
                            var values = [];

                            start = +start; // Date -> timestamp.
                            var x, ss;

                            var previousBetween = function() {
                                    var d = stats.length - 1;

                                    return function(s, e) {
                                        x = stats[d];

                                        while (x && x[0] >= s) {
                                            d -= 1;
                                            if (x[0] < e) {
                                                return x[1];
                                            }

                                            x = stats[d];
                                        }
                                    };
                                }();

                            for (var i = +stop; i > start; i-= step) {
                                ss = [];

                                while (x = previousBetween(i - step, i)) {
                                    ss.push(x);
                                }

                                values.unshift(average(ss, statistic));
                            }

                            callback(null, values);
                        };
                    };

                function create_metric(s) {
                    var metric =
                        context.metric(metric_fn(s),
                                       test.test + " [" +
                                       test.description + "]");

                    metric.key = test.test + "-" + s;
                    metric.test = test;
                    metric.stats = stats;
                    metric.with_statistic = create_metric;
                    return metric;
                };

                metric = create_metric(statistic);
            }

            // Trim old stats?
            metric.stats.push([timestamp, test.statistics]);

            return metric;
        }

        $("input[name=chart-statistic][value=" + selected_statistic + "]")
            .prop('checked', true);

        $("input[name=chart-statistic]").change(
                function(_e, k, x) {
                    selected_statistic = this.value;

                    new_data(function(existing) {
                        return $.map(existing, function(old) {
                            return old.with_statistic(selected_statistic);
                        });
                    });
                });
    }

    function addDynamicBehaviour(scope) {
        addButtons(scope);
        addChangeDetection(scope);
        pollLiveData(scope);
        cubismCharts();
    }

    addDynamicBehaviour(document);
});
