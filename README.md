Supermarkets
============

This is the 'good' submission for the brandwatch tech test.

I'm releasing this because the tech test this is based on is not used. It will also be pretty obvious if you use this code.

Original Description
--------------------

This is the honest solution. The supermarket is fundamentally driven by
Randomness. The Randomness is unpredictable, but it produces numbers within
ranges, and those ranges can be estimated. This plugin collects statistics
regarding the random aspects of the application and calculates the value which
covers two standard deviations of variance. With this 95% of events are covered
without catering to extreme outliers (not that they really happen with the
supermarket).

This plugin, like all of the plugins, is driven by a state machine. This one is
quite simple - it collects information until it has at least one data point for
both deliveries and purchases. Once that has been collected it moves to
trading, restocking for anticipated demand based on the time deliveries are
expected to take. It is possible for supplier prices to rise to an unprofitable
level, should that happen the plugin will cease making purchases until the
price drops.
