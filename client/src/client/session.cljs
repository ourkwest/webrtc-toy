(ns client.session
  (:require [clojure.string :as string]))


(def words
  ;TODO: one per letter of the alphabet (per group)?
  {:quantities ["not any"
                "two" "three" "three and a half" "four" "five" "six" "seven" "eight" "nine" "ten" "eleven" "a dozen"
                "fourty two" "a hundred" "a thousand" "a million" "lots of" "nine and three quarters"
                "six hundred and sixty six" "thirteen" "eighteen" "thirty two" "all the" "infinite" "seventy"]
   :qualities  ["soft" "hard" "fluffy" "squishy" "shiny" "bouncy" "rubbery" "elastic" "chaotic" "melancholy"
                "benevolent" "peaceful" "delightful" "meditating" "malingering" "hungry" "rabid" "memorable" "brave"
                "happy" "lucid" "bouyant" "spinning" "dizzy" "flirtatious" "glamourous" "dangerous" "quantum"
                "electric" "mechanical" "robotic" "pedantic" "annoying" "moronic" "gleeful" "tame" "wild"
                "friendly" "calm" "dazzling" "robust"]
   :colours    ["black" "white" "red" "green" "blue"
                "turquoise" "magenta" "yellow" "violet" "grey" "pink" "purple" "brown" "orange" "golden" "silver"
                "stripey" "spotty" "tangerine" "beige" "scarlet" "crimson" "lilac" "teal" "magnolia" "peach"]
   :nouns      ["armadillos" "baboons" "centipedes" "dormice" "elephants" "frogs" "grasshoppers" "humans" "iguanas"
                "jellyfish" "kangaroos" "lizards" "moles" "newts" "otters" "pussy cats" "quails" "rattlesnakes"
                "stoats" "turkeys" "umbrellas" "velociraptors" "wombats" "x-ray machines" "yaks" "zebras"]})

(defn remove-spaces [x]
  (string/replace x " " "-"))

(def options [(:quantities words)
              (sort (:qualities words))
              (sort (:colours words))
              (sort (:nouns words))])

(defn random-session-id []
  (string/join " " (map rand-nth options)))

(defn schelling-session-id []
  (string/join " " (map first options)))


;(println "Posibilities: " (apply * (map count options)))
;(println "Random: " (random-session-id))
