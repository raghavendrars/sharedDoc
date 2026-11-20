package com.kidslearn.app;

/** Offline quiz questions for Class 1–5: question, right answer, three wrong answers. */
final class QBank {
    private QBank() {
    }

    static final String[][] ENGLISH = {
            {"Pick the noun (a naming word)", "table", "run", "happy", "quickly"},
            {"Pick the verb (an action word)", "jump", "blue", "chair", "soft"},
            {"Pick the adjective (a describing word)", "tall", "sing", "book", "slowly"},
            {"Plural of “child”", "children", "childs", "childes", "childrens"},
            {"Plural of “box”", "boxes", "boxs", "boxen", "boxies"},
            {"Plural of “baby”", "babies", "babys", "babyes", "babis"},
            {"I ate ___ apple.", "an", "a", "am", "and"},
            {"She has ___ cat.", "a", "an", "am", "at"},
            {"They ___ playing football.", "are", "is", "am", "be"},
            {"I ___ a student.", "am", "is", "are", "be"},
            {"He ___ to school every day.", "goes", "go", "going", "gone"},
            {"Past tense of “go”", "went", "goed", "gone", "going"},
            {"Opposite of “hot”", "cold", "warm", "big", "wet"},
            {"Opposite of “happy”", "sad", "glad", "merry", "jolly"},
            {"Same meaning as “big”", "large", "tiny", "short", "thin"},
            {"Which sentence is written correctly?", "She is my friend.", "she is my friend.", "She is my friend", "she is my Friend."},
            {"Which mark ends a question?", "?", ".", ",", "!"},
            {"Which word rhymes with “cat”?", "hat", "cup", "dog", "sun"},
            {"Which word comes first in ABC order?", "apple", "banana", "cherry", "mango"},
            {"Feminine of “king”", "queen", "prince", "man", "lord"},
    };

    static final String[][] EVS = {
            {"Which part of a plant makes its food?", "Leaf", "Root", "Stem", "Flower"},
            {"Which gas do we breathe in to live?", "Oxygen", "Carbon dioxide", "Smoke", "Steam"},
            {"Water turns into ice when it is…", "very cold", "very hot", "windy", "sunny"},
            {"Which sense organ helps us to hear?", "Ears", "Eyes", "Nose", "Tongue"},
            {"Which food helps our body grow strong?", "Dal", "Chips", "Cola", "Candy"},
            {"When should we wash our hands?", "Before eating", "Only on Sundays", "Never", "Only after sleeping"},
            {"Which of these is a healthy snack?", "Fruit", "Chips", "Cola", "Candy"},
            {"Which of these travels on water?", "Ship", "Bus", "Train", "Aeroplane"},
            {"Which season is the hottest?", "Summer", "Winter", "Monsoon", "Autumn"},
            {"Where does rain come from?", "Clouds", "Trees", "Mountains", "Roads"},
            {"How many days are there in a week?", "7", "5", "10", "12"},
            {"What helps keep our teeth clean?", "Brushing", "Eating sweets", "Drinking cola", "Sleeping"},
            {"Where should we throw rubbish?", "In a dustbin", "On the road", "In the river", "Under the bed"},
            {"Which of these is a living thing?", "A tree", "A stone", "A chair", "A pencil"},
            {"From which direction does the Sun rise?", "East", "West", "North", "South"},
            {"What should we wear on a rainy day?", "Raincoat", "Woollen cap", "Sunglasses", "Swimsuit"},
    };

    static final String[][] GK = {
            {"What is the capital of India?", "New Delhi", "Mumbai", "Bengaluru", "Kolkata"},
            {"What is the capital of Karnataka?", "Bengaluru", "Mysuru", "Hubballi", "Mangaluru"},
            {"What is the national animal of India?", "Tiger", "Lion", "Elephant", "Cow"},
            {"What is the national bird of India?", "Peacock", "Parrot", "Crow", "Sparrow"},
            {"What is the national flower of India?", "Lotus", "Rose", "Sunflower", "Jasmine"},
            {"How many colours are there in a rainbow?", "7", "5", "6", "8"},
            {"How many months are there in a year?", "12", "10", "11", "13"},
            {"Which planet do we live on?", "Earth", "Mars", "Jupiter", "Venus"},
            {"Which is the largest ocean?", "Pacific Ocean", "Indian Ocean", "Atlantic Ocean", "Arctic Ocean"},
            {"Who is called the Father of the Nation in India?", "Mahatma Gandhi", "Jawaharlal Nehru", "Subhas Chandra Bose", "Bhagat Singh"},
            {"Which festival is called the festival of lights?", "Deepavali", "Holi", "Ugadi", "Onam"},
            {"What colour is the middle band of the Indian flag?", "White", "Saffron", "Green", "Blue"},
            {"How many players does a cricket team have on the field?", "11", "9", "10", "12"},
            {"Which is the longest river in India?", "Ganga", "Kaveri", "Krishna", "Narmada"},
            {"What do bees make?", "Honey", "Milk", "Silk", "Wool"},
            {"How many hours are there in one day?", "24", "12", "60", "7"},
    };

    static final String[][] ANIMALS = {
            {"Which animal is called the King of the Jungle?", "Lion", "Tiger", "Elephant", "Bear"},
            {"Which is the largest animal in the world?", "Blue whale", "Elephant", "Giraffe", "Shark"},
            {"Which animal has a very long neck?", "Giraffe", "Zebra", "Horse", "Camel"},
            {"Which animal is called the Ship of the Desert?", "Camel", "Horse", "Elephant", "Donkey"},
            {"How many legs does a spider have?", "8", "6", "10", "4"},
            {"How many legs does an insect have?", "6", "8", "4", "10"},
            {"A baby cow is called a…", "calf", "cub", "kitten", "puppy"},
            {"A baby dog is called a…", "puppy", "calf", "kitten", "foal"},
            {"A baby frog is called a…", "tadpole", "caterpillar", "chick", "cub"},
            {"Where does a bird live?", "Nest", "Den", "Kennel", "Stable"},
            {"Where does a lion live?", "Den", "Nest", "Hive", "Kennel"},
            {"Which animal gives us wool?", "Sheep", "Cow", "Hen", "Dog"},
            {"Which of these can fly?", "Eagle", "Penguin", "Ostrich", "Dog"},
            {"Which animal lives in water?", "Fish", "Cat", "Dog", "Cow"},
            {"Which animal has black and white stripes?", "Zebra", "Tiger", "Giraffe", "Leopard"},
            {"Which of these is a mammal?", "Dog", "Frog", "Snake", "Parrot"},
    };

    static final String[][] PLANTS = {
            {"Which part of a plant holds it in the soil?", "Root", "Leaf", "Flower", "Fruit"},
            {"Which part of a plant makes its food?", "Leaf", "Root", "Stem", "Seed"},
            {"What do plants need to grow?", "Water and sunlight", "Only sugar", "Only darkness", "Milk"},
            {"What colour are most leaves?", "Green", "Blue", "Black", "Purple"},
            {"Which part of a plant carries water up?", "Stem", "Flower", "Fruit", "Seed"},
            {"A new plant grows from a…", "seed", "stone", "feather", "shell"},
            {"Which of these is a fruit?", "Mango", "Carrot", "Potato", "Spinach"},
            {"Which vegetable is a root we eat?", "Carrot", "Apple", "Banana", "Grapes"},
            {"What is the national tree of India?", "Banyan", "Neem", "Mango", "Peepal"},
            {"Which plant grows in the desert?", "Cactus", "Lotus", "Rice", "Tea"},
            {"Which part of a plant turns into a fruit?", "Flower", "Root", "Stem", "Leaf"},
            {"Which plant has thorns?", "Rose", "Lotus", "Tulsi", "Grass"},
            {"Which gas do plants give out in sunlight?", "Oxygen", "Carbon dioxide", "Smoke", "Nitrogen"},
            {"Which plant floats on water?", "Lotus", "Mango", "Cactus", "Coconut"},
            {"Which is a climber that needs support to grow?", "Money plant", "Mango tree", "Coconut tree", "Banyan tree"},
    };
}
