(ns client.session
  (:require [clojure.string :as string]))


(def words
  {:quantities ["not any"
                "two" "three" "three and a half" "four" "five" "six" "seven" "eight" "nine" "ten" "eleven" "twelve"
                "fourty two" "a hundred" "a thousand" "a million" "lots of"]
   :qualities  ["soft" "hard" "fluffy" "squishy" "shiny" "bouncy" "rubbery" "elastic" "chaotic" "melancholy"
                "benevolent" "peaceful" "delightful" "meditating" "malingering" "hungry" "rabid" "memorable" "brave"
                "happy" "lucid" "bouyant" "spinning" "dizzy" "flirtatious" "glamourous" "dangerous" "quantum"
                "electric" "mechanical" "robotic" "pedantic" "annoying" "moronic" "gleeful" "tame" "wild"
                "friendly" "calm" "dazzling" "robust"]
   :colours    ["black" "white" "red" "green" "blue" "turquoise" "magenta" "yellow"
                "pink" "purple" "brown" "grey" "golden" "silver"
                "stripey" "spotty"]
   :nouns      ["bunnies" "lions" "bears" "penguins" "snakes" "eagles" "hedgehogs" "owls" "pussy cats" "rats" "mice"
                "monkeys" "humans" "robins" "weevils" "chickens" "peacocks" "velociraptors" "octopuses" "hippos"
                "rhinos" "hyenas" "moles" "bats" "bananas"]})

(defn remove-spaces [x]
  (string/replace x " " "-"))

(def options [(:quantities words)
              (sort (:qualities words))
              (sort (:colours words))
              (sort (:nouns words))])

(defn random-session-id []
  (string/join " " (map rand-nth options)))


;(println "Posibilities: " (apply * (map count options)))
;(println "Random: " (random-session-id))
