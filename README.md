## Implementation of AI models playing MicroRTS

This is an implementation of two RTS game AI models using the MicroRTS framework written by microRTS
(see [Github repository](https://github.com/santiontanon/microrts) or [oficial MicroRTS website](https://sites.google.com/site/micrortsaicompetition/home)) 

Models (located in *src/ai/evolution/*, run by *src/gui/frontend/EvolutionUI*): 
- Genetic programming model
- NEAT model (implementation is based on [evo-NEAT](https://github.com/vishnugh/evo-NEAT))

### Training of the model
Setup in src/ai/evolution/utils/*TrainingUtils*:

Setup training mode by:
```kotlin
val MODE = Mode.TRAINING
```

Choose which model do you want to use:
```kotlin
val AI = TrainAI.NEAT // for NEAT model                    
val AI = TrainAI.GP // for GP model
val AI = TrainAI.GP_STRATEGY // for GP model with strategy AI on top
```

Setup training parameters and choose AI(s) to train against (each AI is run only once):
```kotlin
const val EPOCH_COUNT = 10 // Number of generations
const val POPULATION = 100 // Size of a population
val FITNESS = FitnessType.BASIC // type of fitness to use for evaluation

// ... See TrainingUtils for more parameters with descriptions

fun getTrainingAI(): List<String> = mutableListOf(
    "ai.RandomBiasedAI",
    "ai.RandomBiasedAI",
)
```

For GP model only:
```kotlin
const val CONDITION_COUNT = 10 // number of conditions per individual
const val COND_MUT_PROB = 0.14 // probability of mutation
...
```

For GP with Strategy in utils/StrategyTrainingUtils:
```kotlin
const val CONDITION_COUNT = 10 // number of strategy conditions additionaly to regular ones
```

For NEAT model only:
```kotlin
const val HIDDEN_UNITS = 100000 // number of hidden units of networks
```
**Note**: you can also change mutation probabilities and other parameters of NEAT directly in src/ai/evolution/neat/NEAT_config

Add "EVOLUTION" value in resources/config.properties file and run application from MicroRTS class using gradle:
```kotlin
launch_mode=EVOLUTION
```
Once the training is finished, 
the population, training progress and best candidate 
solution will be stored in the folder:
```kotlin
val ROOT_OUTPUT_FOLDER = "output/${TrainingUtils.AI.name}_${TrainingUtils.POPULATION}_map=${TrainingUtils.MAP_WIDTH}_${TrainingUtils.getActiveAIS()}" // + other specific parameters for the model run
```

### Testing
After training, you can test your model.

in *TrainingUtils*, setup variables:
```kotlin
val MODE = Mode.TESTING
val AI = TrainAI.NEAT // replace with AI type that you want to test
```

and in TestingUtils setup:
```kotlin
 val TEST_FILE = File("name of the file with saved model")
```

Choose AIs to test against, their budget size and number of runs for each AI:
```kotlin
fun getTestingAIs(): MutableList<String> = mutableListOf(
            "ai.RandomBiasedAI",
            "ai.mcts.informedmcts.InformedNaiveMCTS",
    )
const val TESTING_RUNS = 100
const val TESTING_BUDGET = 100
```
Run the same way as training.

### Tournament
Tournament testing allows to play games of two trained models that are both loaded from file.

in *TrainingUtils*, setup variables:
```kotlin
val MODE = Mode.TOURNAMENT_TESTING 
```

Choose files to load both AIs from and define what type of AI they are in TestingUtils:
```kotlin
val AI_1_TYPE = TrainingUtils.TrainAI.GP
val AI_1 = File("file_with_model_1")

val AI_2_TYPE = TrainingUtils.TrainAI.NEAT
val AI_2 = File("file_with_model_2")
```

Choose number of games in TestingUtils:
```kotlin
const val TESTING_RUNS = 100
```

Run the same way as training.

### MicroRTS notes

**Changes in MicroRTS framework to better suite the needs of these models**:
- Addition of statistics during game play in order to gather data for fitness evaluation
- Changes in MicroRTS class in order to run TrainingUI (adding option of "EVOLUTION")
- Adding an option to setup computational budget of AIs during creation of an instance
- Moving setting of basic MicroRTS parameters into TrainingUtils
- Added gradle and uberjar build option

### evo-NEAT notes
Location: src/evolution/evoneat

**Changes in evo-NEAT model**:
- removal of unused classes/packages for our purposes
- Changing of parameter values in NEAT_Config file