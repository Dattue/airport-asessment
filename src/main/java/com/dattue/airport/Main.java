package com.dattue.airport;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

	public static final String VERSION = "v1.0.0";
	
	/**
	 * Check arguments to determine running interactive or command mode
	 */
	public static void main(String[] args) throws IOException {

		// Error output check
		if (System.err == null) {
			System.out.println("Console error output is not usable. All errors will be printed through the standard output");
			System.setErr(System.out);
		}
		
		if (args == null || args.length == 0) { 
			System.out.println("/-------------------/");
			System.out.println("/      AIRPORT      /");
			System.out.println("/-------------------/");
			System.out.printf("%20s%n", VERSION);
			System.out.println(" > INTERACTIVE MODE");
			System.out.println();
			interactiveMode();
		} else if (args.length < 4) {
			System.err.println("Please provide all params as arguments:");
			System.err.println("airport <airportsCSVRoute> <countriesCSVRoute> <runwaysCSVRoute> <country>");
		} else {
			System.out.println();

			// Don't check runways.csv or countryCode as it is unnecessary for calculating the airports with most runways
			String airportsRoute = pullUpFile(args[0], "airports.csv");
			String countriesRoute = pullUpFile(args[1], "countries.csv");
			
			if (airportsRoute == null || countriesRoute == null) {
				System.err.println("Cannot proceed. Please check the files specified as arguments");
				System.exit(1);
			}
			
			Map<String, Integer> countedAirports = calcMostAirports(airportsRoute, countriesRoute);
			System.out.println("Top 10 countries with the most airports");
			printAirportRanking(reverseOrderMapByIntegerValue(countedAirports), 10);
			System.out.println();

			String runwaysRoute = pullUpFile(args[2], "runways.csv");
			
			if (runwaysRoute == null) {
				System.err.println("Cannot proceed. Please check the file specified as arguments");
				System.exit(1);
			}

			queryRunwaysFromCountry(airportsRoute, countriesRoute, runwaysRoute, args[3]);
		}

	}
	
	/**
	 * View and logic for INTERACTIVE MODE
	 *  
	 */
	private static void interactiveMode() throws IOException {
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		String textInput = null;
		
		String airportCSVRoute = askForFile("You may now specify the route of the airports.csv to use:", "airports.csv");
		String countryCSVRoute = askForFile("OK. You may now specify the route of the countries.csv to use:", "countries.csv");
		String runwayCSVRoute = askForFile("OK. You may now specify the route of the runways.csv to use:", "runways.csv");
		
		System.out.println("OK. What would you like to do?");
		
		do {
			try {
				if (textInput == null || textInput.trim().isEmpty()) {
					System.out.println("You can use the number as well as the word in CAPS to select an option:");
					System.out.println("1. TOP ten countries with highest number of airports");
					System.out.println("2. RUNWAYS for each airport given a country code or country name");
					System.out.println("3. EXIT");

				} else {
					System.out.println();
					System.out.println("OK. Something else? (1. TOP; 2. RUNWAYS; 3. EXIT)");
				}

				System.out.print("> ");

				textInput = inputReader.readLine().trim();

				if (textInput.equals("1") || textInput.equalsIgnoreCase("TOP")) {
					Map<String, Integer> countedAirports = calcMostAirports(airportCSVRoute, countryCSVRoute);
					System.out.println("Top 10 countries with the most airports");
					printAirportRanking(reverseOrderMapByIntegerValue(countedAirports), 10);

				} else if (textInput.equals("2") || textInput.equalsIgnoreCase("RUNWAYS")) {
					System.out.println("OK. For which country? You may query the exact country code or country name");
					System.out.print("> ");
					textInput = inputReader.readLine().trim();
					try {
						queryRunwaysFromCountry(airportCSVRoute, countryCSVRoute, runwayCSVRoute, textInput);
					} catch (IllegalArgumentException iaex) {
						System.err.println(iaex);
					}
				}

			} catch (IOException ioex) {
				System.err.println("The provided input could not be processed");
			}

		} while (!textInput.equals("3") && !textInput.equalsIgnoreCase("EXIT"));

		System.out.println("BYE");
	}

	/**
	 * Routine for INTERACTIVE MODE to request file input
	 * 
	 *  @param Message Message to show when asking for files
	 *  @param optionalFile Which file to look for if the specified route is a directory
	 */
	private static String askForFile(String message, String optionalFile) {
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		String textInput = null;
		String route = null;

		do {
			try {
				if (textInput == null || textInput.trim().isEmpty()) {
					System.out.println(message);
				}

				System.out.print("> ");
				textInput = inputReader.readLine();
				route = pullUpFile(textInput, optionalFile);

			} catch (IOException ioex) {
				System.err.println("The provided input could not be processed");
			}

		} while (route == null);

		return route;
	}

	/**
	 *  Returns route String when the file in the provided route is correctly formed and
	 *  the file specified exists, is readable and has a minimum length.
	 * 
	 *  If a directory is specified instead it will check inside that directory for
	 *  optionalFile and run the checks in it instead
	 *  
	 * @param route  
	 * @param optionalFile Which file to look for if the specified route is a directory
	 * @throws IOException
	 */
	private static String pullUpFile(String route, String optionalFile) throws IOException {

		try {

			Path file = new File(route).toPath().toAbsolutePath();

			if (Files.isDirectory(file)) {
				file = Paths.get(route, optionalFile).toAbsolutePath();
			}

			if (Files.exists(file)) {

				if (!Files.isReadable(file)) {
					System.err.println("The specified file in " + file + " could not be read");
				}

				if (Files.isRegularFile(file) && (Files.lines(file).count() < 3 || Files.size(file) < 10)) {
					System.err.println("The specified file is not a valid data source: \n" + file);
				}

				return file.toString();
			} else {
				System.err.println("No file has been found at \n" + file);
			}
		} catch (IOException ioex) {
			System.err.println("The specified file in " + route + " could not be read");
		}
		
		return null;
	}
	
	/**
	 * Order map having the first elements being the ones with the largest integer values
	 * Used in creating the top 10 countries with most airports
	 * 
	 */
	private static Map<String, Integer> reverseOrderMapByIntegerValue(Map<String, Integer> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
		 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(a, b) -> a, LinkedHashMap::new));
	}

	private static void printAirportRanking(Map<String, Integer> sortedMap, int limit) {
		Iterator<Map.Entry<String, Integer>> it = sortedMap.entrySet().iterator();

		for (int i = 1; i <= limit; i++) {
			if (it.hasNext()) {
				Entry<String, Integer> entry = it.next();
				System.out.println(i + ". " + entry.getKey() + ": " + entry.getValue());
			} else {
				// If there's less than 10 items, exit prematurely to avoid NSEEX
				break;
			}
		}
	}

	/**
	 * Given an airport CSV and a country CSV,
	 * collect in a map which country has the most airports
	 * 
	 * @return Unordered Map with country code as key and count of airports as value
	 */
	private static Map<String, Integer> calcMostAirports(String airportsCSVRoute, String countriesCSVRoute)
			throws IOException {
		Map<String, String> countries = new LinkedHashMap<>();

		try (Stream<String> stream = Files.lines(Paths.get(countriesCSVRoute))) {

			Iterator<String> iterator = stream.iterator();

			String header = iterator.next();

			int codeIndex = Arrays.asList(header.split(",")).indexOf("\"code\"");
			int countryIndex = Arrays.asList(header.split(",")).indexOf("\"name\"");

			if (codeIndex == -1 || countryIndex == -1) {
				throw new IllegalArgumentException("The column 'code' or 'name' are absent in the specified file "
						+ countriesCSVRoute);
			}

			while (iterator.hasNext()) {
				String line = iterator.next();

				String code = line.split(",")[codeIndex].trim().replaceAll("^\"|\"$", "");
				String country = line.split(",")[countryIndex].trim().replaceAll("^\"|\"$", "");

				countries.put(code, country);
			}

		} catch (IOException ioex) {
			System.err.println("Country CSV could not be read. Cause: " + ioex.getMessage());
			throw ioex;
		}

		
		Map<String, Integer> countryAirportMap = countries.keySet().stream()
				.collect(Collectors.toMap(Function.identity(), k -> 0));
		
		
		try (Stream<String> stream = Files.lines(Paths.get(airportsCSVRoute))) {

			Iterator<String> iterator = stream.iterator();

			String header = iterator.next();

			int codeIndex = Arrays.asList(header.split(",")).indexOf("\"iso_country\"");

			if (codeIndex == -1) {
				throw new IllegalArgumentException("The column 'iso_country' in absent in the specified file "
						+ airportsCSVRoute);
			}

			while (iterator.hasNext()) {
				String line = iterator.next();

				String code = line.split(",")[codeIndex].trim().replaceAll("^\"|\"$", "");

				if (countryAirportMap.containsKey(code)) {
					countryAirportMap.replace(code, countryAirportMap.get(code) + 1);
				}
			}

		} catch (IOException ioex) {
			System.err.println("Airport CSV could not be read. Cause: " + ioex.getMessage());
			throw ioex;
		}

		return countryAirportMap;
	}

	/**
	 * Given an country two letter ISO code or name and routes for airports.csv,
	 * countries.csv and runways.csv retrieve every runway for each airport in the
	 * given country and print it through standard input
	 * 
	 */
	private static void queryRunwaysFromCountry(String airportsCSVRoute, String countriesCSVRoute,
			String runwaysCSVRoute, String queriedCountry) throws IOException {

		queriedCountry = queriedCountry.trim().replaceAll("^\"|\"$", "");

		String codeRow = null;
		String countryRow = null;

		try (Stream<String> stream = Files.lines(Paths.get(countriesCSVRoute))) {

			Iterator<String> iterator = stream.iterator();

			String header = iterator.next();

			int codeIndex = Arrays.asList(header.split(",")).indexOf("\"code\"");
			int countryIndex = Arrays.asList(header.split(",")).indexOf("\"name\"");

			if (codeIndex == -1 || countryIndex == -1) {
				throw new IllegalArgumentException("The column 'code' or 'name' are absent in the specified file " + countriesCSVRoute);
			}

			while (iterator.hasNext()) {
				String line = iterator.next();

				codeRow = line.split(",")[codeIndex].trim().replaceAll("^\"|\"$", "");
				countryRow = line.split(",")[countryIndex].trim().replaceAll("^\"|\"$", "");

				if (queriedCountry.equals(codeRow) || queriedCountry.equalsIgnoreCase(countryRow)) {
					break; // Exit as soon as a match is found
				}

			}

			if (!queriedCountry.equals(codeRow) && !iterator.hasNext()) {
				throw new IllegalArgumentException("No match on countries with '" + queriedCountry + "' has been found."
						+ " Airport CSV and Runway CSV will not be processed.");
			}

		} catch (IOException ioex) {
			System.err.println("Country CSV could not be read. Cause: " + ioex.getMessage());
			throw ioex;
		}

		Map<String, String> airportsMap = new HashMap<>();

		try (Stream<String> stream = Files.lines(Paths.get(airportsCSVRoute))) {

			Iterator<String> iterator = stream.iterator();

			String header = iterator.next();

			int countryIndex = Arrays.asList(header.split(",")).indexOf("\"iso_country\"");
			int aiportIdIndex = Arrays.asList(header.split(",")).indexOf("\"id\"");
			int aiportNameIndex = Arrays.asList(header.split(",")).indexOf("\"name\"");

			if (countryIndex == -1 || aiportIdIndex == -1 || aiportNameIndex == -1) {
				throw new IllegalArgumentException("The column 'iso_country', 'id' or 'name' are absent in the specified file "
						+ airportsCSVRoute);
			}

			while (iterator.hasNext()) {
				String line = iterator.next();

				String isoCountry = line.split(",")[countryIndex].trim().replaceAll("^\"|\"$", "");
				String id = line.split(",")[aiportIdIndex].trim().replaceAll("^\"|\"$", "");
				String airportName = line.split(",")[aiportNameIndex].trim().replaceAll("^\"|\"$", "");

				if (isoCountry.equals(codeRow)) {
					airportsMap.put(id, airportName);
				}

			}

		} catch (IOException ioex) {
			System.err.println("Airport CSV could not be read. Cause: " + ioex.getMessage());
			throw ioex;
		}

		if (airportsMap.isEmpty()) {
			System.out.println("The country '" + countryRow + "' (" + codeRow + ") has no airports registered.");
			System.out.println("Runway CSV will not be processed");
			return;
		}

		Map<String, List<String>> airportRunways = airportsMap.keySet().stream()
				.collect(Collectors.toMap(Function.identity(), k -> new ArrayList<String>()));

		try (Stream<String> stream = Files.lines(Paths.get(runwaysCSVRoute))) {

			Iterator<String> iterator = stream.iterator();

			String header = iterator.next();

			int airportIndex = Arrays.asList(header.split(",")).indexOf("\"airport_ref\"");
			int runwayIdIndex = Arrays.asList(header.split(",")).indexOf("\"id\"");

			if (airportIndex == -1 || runwayIdIndex == -1 ) {
				throw new IllegalArgumentException("The column 'airport_ref', 'id' or 'name' are absent in the specified file "
						+ runwaysCSVRoute);
			}

			while (iterator.hasNext()) {
				String line = iterator.next();

				String airport = line.split(",")[airportIndex].trim().replaceAll("^\"|\"$", "");
				String runway = line.split(",")[runwayIdIndex].trim().replaceAll("^\"|\"$", "");

				if (airportRunways.containsKey(airport)) {
					List<String> runwayList = airportRunways.get(airport);
					runwayList.add(runway);

					airportRunways.replace(runway, runwayList);
				}
			}

		} catch (IOException ioex) {
			System.err.println("Runway CSV could not be read. Cause: " + ioex.getMessage());
			throw ioex;
		}

		System.out.println("Runways IDs for the following aiports in " + countryRow + " (" + codeRow + "):");
		for (Map.Entry<String, String> airport : airportsMap.entrySet()) {
			System.out.println("* " + airport.getValue() + " (airportID: " + airport.getKey() + "): "
					+ airportRunways.get(airport.getKey()));
		}

	}

}
