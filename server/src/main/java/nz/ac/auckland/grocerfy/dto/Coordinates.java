package nz.ac.auckland.grocerfy.dto;
/**
 * Coordinates that will be used to identify the location of a store. This is used to calculate distances between stores and the user.
 */
public record Coordinates(double latitude, double longitude) {}