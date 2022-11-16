package com.dattue.airport;

import java.io.IOException;
import java.nio.file.Files;
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

	public static void main(String[] args) throws IOException {
		
		System.out.println("/-------------------/");
		System.out.println("/      AIRPORT      /");
		System.out.println("/-------------------/");
		
		if (args == null || args.length < 4) {
			System.out.println("Please provide following params as arguments: ");
			System.out.println("airport <airportsCSVRoute> <countriesCSVRoute> <runwaysCSVRoute> <countryCode>");
			return;
		}
		
		Map<String, Integer> countedAirports = calcMostAirports(args[0], args[1]);
		System.out.println("Top 10 countries with the most airports");
		printAirportRanking(reverseOrderMapByIntegerValue(countedAirports), 10);
		
		System.out.println();
		
		queryRunwaysFromCountry(args[0], args[1], args[2], args[3]);
		
	}
	
	private static Map<String, Integer> reverseOrderMapByIntegerValue(Map<String, Integer> map) {
		return map.entrySet().stream()
		.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
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
	
	private static Map<String, Integer> calcMostAirports(String airportsCSVRoute, String countriesCSVRoute) throws IOException {
		Map<String, String> countries = new LinkedHashMap<>();
		
		try(Stream<String> stream = Files.lines(Paths.get(countriesCSVRoute))) {
			
			Iterator<String> iterator = stream.iterator();
			
			// Dividir por el separador
			
			String header = iterator.next();
			
			int codeIndex = Arrays.asList(header.split(",")).indexOf("\"code\"");
			int countryIndex = Arrays.asList(header.split(",")).indexOf("\"name\"");
			
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

		// MAP creation
		
		Map<String, Integer> countryAirportMap = countries.keySet().stream()
				.collect(Collectors.toMap(Function.identity(), k -> 0));
		
		try (Stream<String> stream = Files.lines(Paths.get(airportsCSVRoute))) {
			
			Iterator<String> iterator = stream.iterator();
			
			String header = iterator.next();
			
			int codeIndex = Arrays.asList(header.split(",")).indexOf("\"iso_country\"");
			
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
	
	private static void queryRunwaysFromCountry(String airportsCSVRoute, String countriesCSVRoute,
			String runwaysCSVRoute, String queriedCountry) throws IOException {
		
		queriedCountry = queriedCountry.trim().trim().replaceAll("^\"|\"$", "");
		
		String codeRow = null;
		String countryRow = null;

		try (Stream<String> stream = Files.lines(Paths.get(countriesCSVRoute))) {

			Iterator<String> iterator = stream.iterator();

			// Dividir por el separador

			String header = iterator.next();

			int codeIndex = Arrays.asList(header.split(",")).indexOf("\"code\"");
			int countryIndex = Arrays.asList(header.split(",")).indexOf("\"name\"");

			while (iterator.hasNext()) {
				String line = iterator.next();

				codeRow = line.split(",")[codeIndex].trim().replaceAll("^\"|\"$", "");
				countryRow = line.split(",")[countryIndex].trim().replaceAll("^\"|\"$", "");

				if (queriedCountry.equals(codeRow) || queriedCountry.equalsIgnoreCase(countryRow)) {
					break; // Exit as soon as a match is found
				}
			}

			if (codeRow == null || codeRow.isBlank()) {
				throw new IllegalArgumentException("No match on countries with '" + queriedCountry + "' has been found."
						+ "Airport CSV and Runway CSV will not be processed.");
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
			System.out.println("The country " + countryRow + " (" + codeRow + ") has no airports registered.");
			System.out.println("Runway CSV will not be processed");
			return;
		}
		
		Map<String, List<String>> airportRunways =
				airportsMap.keySet().stream()
				.collect(Collectors.toMap(Function.identity(), k -> new ArrayList<String>()));
				
				new HashMap<String, List<String>>();
		
		try (Stream<String> stream = Files.lines(Paths.get(runwaysCSVRoute))) {
			
			Iterator<String> iterator = stream.iterator();

			// Dividir por el separador

			String header = iterator.next();

			int airportIndex = Arrays.asList(header.split(",")).indexOf("\"airport_ref\"");
			int runwayIdIndex = Arrays.asList(header.split(",")).indexOf("\"id\"");
			
			while(iterator.hasNext()) {
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

