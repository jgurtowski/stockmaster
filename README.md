# Demo: http://pulsarequity.com

Calculates various Options return and value metrics



# Design
## Requirements

Calculates Return on Extrinsic for puts
Displays other useful greeks
Quickly switch ticker
Accessible from multiple devices


## Solution
Clojure development has been rapid and robust for me in the past.

javafx? - played around with it, difficult to work with from Clojure, design tooling seems more mature for web platforms. 

Web based app allows accessibility from multiple devices. Performance is not a concern.

Clojurescript + reframe - good middle ground. Working directly with reagent is a little low level.
