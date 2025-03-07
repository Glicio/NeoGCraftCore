package dev.glicio.utils;

public class StringIntParser {
    public static int convertStringToInt(String input) throws IllegalArgumentException {
        try {
            // Check if the input is a valid number
            double value = Double.parseDouble(input);

            // Scale the value by 100 and convert it to an integer

            return (int) Math.round(value * 100);
        } catch (NumberFormatException e) {
            // Throw an error if the input is invalid
            throw new IllegalArgumentException("Invalid input: " + input);
        }
    }
}
