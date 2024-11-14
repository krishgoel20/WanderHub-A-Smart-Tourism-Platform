
import java.util.*;

class City {
    String name;
    double sunExposure;
    double fitness;
    double sightseeing;
    double nightlife;
    double healthRisks;

    public City(String name, double sunExposure, double fitness, double sightseeing,
                double nightlife, double healthRisks) {
        this.name = name;
        this.sunExposure = sunExposure;
        this.fitness = fitness;
        this.sightseeing = sightseeing;
        this.nightlife = nightlife;
        this.healthRisks = healthRisks;
    }

    public double calculateUtility(UserPreferences preferences) {
        return -preferences.sunExposureWeight * this.sunExposure +
               preferences.fitnessWeight * this.fitness +
               preferences.sightseeingWeight * this.sightseeing +
               preferences.nightlifeWeight * this.nightlife -
               preferences.healthWeight * this.healthRisks;
    }
}

class UserPreferences {
    double sunExposureWeight;
    double fitnessWeight;
    double sightseeingWeight;
    double nightlifeWeight;
    double healthWeight;

    public UserPreferences(double sunExposureWeight, double fitnessWeight,
                          double sightseeingWeight, double nightlifeWeight, double healthWeight) {
        this.sunExposureWeight = sunExposureWeight;
        this.fitnessWeight = fitnessWeight;
        this.sightseeingWeight = sightseeingWeight;
        this.nightlifeWeight = nightlifeWeight;
        this.healthWeight = healthWeight;
    }
}

class IntelligentTouristAgent {
    private Random random = new Random();

    private List<City> generateRandomItinerary(List<City> availableCities, int minCities) {
        if (availableCities.isEmpty()) {
            return new ArrayList<>();
        }
        int cityCount = random.nextInt(Math.max(minCities, availableCities.size()) - minCities + 1) + minCities;
        cityCount = Math.min(cityCount, availableCities.size());
       
        List<City> cities = new ArrayList<>(availableCities);
        Collections.shuffle(cities);
        return cities.subList(0, cityCount);
    }

    private double calculateItineraryUtility(List<City> itinerary, UserPreferences preferences) {
        if (itinerary.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }

        // Check for duplicates
        Set<City> uniqueCities = new HashSet<>(itinerary);
        if (uniqueCities.size() != itinerary.size()) {
            return Double.NEGATIVE_INFINITY;
        }

        double baseUtility = itinerary.stream()
                .mapToDouble(city -> city.calculateUtility(preferences))
                .sum();
        double sizePenalty = itinerary.size() * 0.1;
        return baseUtility - sizePenalty;
    }

    private double acceptanceProbability(double currentUtility, double neighborUtility, double temperature) {
        if (neighborUtility > currentUtility) {
            return 1.0;
        }
        return Math.exp((neighborUtility - currentUtility) / temperature);
    }

    public Map<Integer, List<City>> optimizeCompleteItinerary(UserPreferences preferences,
            List<City> cities, int numDays, int populationSize, int generations,
            double temperature, double coolingRate) {
       
        class ItineraryGenerator {
            Map<Integer, List<City>> generateCompleteItinerary() {
                List<City> remainingCities = new ArrayList<>(cities);
                Map<Integer, List<City>> itinerary = new HashMap<>();
                int minCitiesPerDay = Math.max(1, cities.size() / numDays);

                for (int day = 1; day <= numDays; day++) {
                    if (remainingCities.isEmpty()) {
                        itinerary.put(day, new ArrayList<>());
                        continue;
                    }

                    if (day == numDays) {
                        itinerary.put(day, new ArrayList<>(remainingCities));
                    } else {
                        int maxPossible = Math.max(minCitiesPerDay,
                            remainingCities.size() - (numDays - day));
                        int count = Math.min(remainingCities.size(),
                            random.nextInt(maxPossible - minCitiesPerDay + 1) + minCitiesPerDay);
                       
                        List<City> selected = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            int index = random.nextInt(remainingCities.size());
                            selected.add(remainingCities.remove(index));
                        }
                        itinerary.put(day, selected);
                    }
                }
                return itinerary;
            }
        }

        ItineraryGenerator generator = new ItineraryGenerator();

        // Initialize population
        List<Map<Integer, List<City>>> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(generator.generateCompleteItinerary());
        }

        Map<Integer, List<City>> bestItinerary = null;
        double bestUtility = Double.NEGATIVE_INFINITY;

        for (int generation = 0; generation < generations; generation++) {
            // Sort population by utility
            population.sort((a, b) -> Double.compare(
                calculateCompleteUtility(b, preferences),
                calculateCompleteUtility(a, preferences)
            ));

            // Update best itinerary
            double currentBestUtility = calculateCompleteUtility(population.get(0), preferences);
            if (currentBestUtility > bestUtility) {
                bestUtility = currentBestUtility;
                bestItinerary = new HashMap<>(population.get(0));
            }

            // Create new population
            List<Map<Integer, List<City>>> newPopulation = new ArrayList<>(
                population.subList(0, populationSize / 2));

            // Fill rest with new random itineraries
            while (newPopulation.size() < populationSize) {
                newPopulation.add(generator.generateCompleteItinerary());
            }

            // Apply Simulated Annealing
            for (int i = 0; i < populationSize; i++) {
                Map<Integer, List<City>> neighbor = generator.generateCompleteItinerary();
                double neighborUtility = calculateCompleteUtility(neighbor, preferences);
                double currentUtility = calculateCompleteUtility(newPopulation.get(i), preferences);

                if (acceptanceProbability(currentUtility, neighborUtility, temperature) >
                    random.nextDouble()) {
                    newPopulation.set(i, neighbor);
                }
            }

            temperature *= coolingRate;
            population = newPopulation;
        }

        return bestItinerary != null ? bestItinerary : population.get(0);
    }

    private double calculateCompleteUtility(Map<Integer, List<City>> completeItinerary,
                                          UserPreferences preferences) {
        double totalUtility = 0;
        for (List<City> dayItinerary : completeItinerary.values()) {
            double dayUtility = calculateItineraryUtility(dayItinerary, preferences);
            if (dayUtility == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }
            totalUtility += dayUtility;
        }
        return totalUtility;
    }
}

public class ItineraryOptimizer {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("Enter your preferences on a scale of 0.0 to 1.0:");
        System.out.print("Sun Exposure Weight: ");
        double sunExposureWeight = input.nextDouble();
        System.out.print("Fitness Weight: ");
        double fitnessWeight = input.nextDouble();
        System.out.print("Sightseeing Weight: ");
        double sightseeingWeight = input.nextDouble();
        System.out.print("Nightlife Weight: ");
        double nightlifeWeight = input.nextDouble();
        System.out.print("Health Weight: ");
        double healthWeight = input.nextDouble();

        UserPreferences preferences = new UserPreferences(
            sunExposureWeight, fitnessWeight, sightseeingWeight,
            nightlifeWeight, healthWeight
        );

        System.out.print("Enter the Population Size for the Genetic Algorithm: ");
        int populationSize = input.nextInt();
        System.out.print("Enter the Number of Generations for optimization: ");
        int generations = input.nextInt();
        System.out.print("Enter the Initial Temperature for Simulated Annealing: ");
        double temperature = input.nextDouble();
        System.out.print("Enter the Cooling Rate for Simulated Annealing (0.0 to 1.0): ");
        double coolingRate = input.nextDouble();
        System.out.print("Enter the Number of Days for the Itinerary: ");
        int numDays = input.nextInt();

        System.out.print("Enter the Number of Cities: ");
        int numCities = input.nextInt();
        input.nextLine();

        List<City> cities = new ArrayList<>();
        for (int i = 0; i < numCities; i++) {
            System.out.println("\nEnter the details for City " + (i + 1) + ":");
            System.out.print("City Name: ");
            String name = input.nextLine();
            System.out.print("Sun Exposure (0.0 to 1.0): ");
            double sunExposure = input.nextDouble();
            System.out.print("Fitness (0.0 to 1.0): ");
            double fitness = input.nextDouble();
            System.out.print("Sightseeing (0.0 to 1.0): ");
            double sightseeing = input.nextDouble();
            System.out.print("Nightlife (0.0 to 1.0): ");
            double nightlife = input.nextDouble();
            System.out.print("Health Risks (0.0 to 1.0): ");
            double healthRisks = input.nextDouble();
            input.nextLine();

            cities.add(new City(name, sunExposure, fitness, sightseeing, nightlife, healthRisks));
        }

        IntelligentTouristAgent agent = new IntelligentTouristAgent();
        Map<Integer, List<City>> completeItinerary = agent.optimizeCompleteItinerary(
            preferences, cities, numDays, populationSize, generations, temperature, coolingRate
        );

        System.out.println("\nBest Itinerary:");
        for (int day = 1; day <= numDays; day++) {
            System.out.println("\nDay " + day + ":");
            List<City> dayItinerary = completeItinerary.get(day);
            if (dayItinerary != null && !dayItinerary.isEmpty()) {
                dayItinerary.sort((city1, city2) -> Double.compare(city2.calculateUtility(preferences), 
                city1.calculateUtility(preferences)));
                for (City city : dayItinerary) {
                    System.out.println(city.name);
                }
            } else {
                System.out.println("Rest day or no cities available");
            }
        }

        input.close();
    }
}
