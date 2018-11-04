/*
 * This file is part of Openrouteservice.
 *
 * Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, see <https://www.gnu.org/licenses/>.
 */

package heigit.ors.api.requests.matrix;

import com.vividsolutions.jts.geom.Coordinate;
import heigit.ors.api.requests.common.APIEnums;
import heigit.ors.common.DistanceUnit;
import heigit.ors.exceptions.ParameterValueException;
import heigit.ors.exceptions.StatusCodeException;
import heigit.ors.matrix.MatrixErrorCodes;
import heigit.ors.matrix.MatrixResult;
import heigit.ors.routing.RoutingProfileManager;
import heigit.ors.routing.RoutingProfileType;
import heigit.ors.util.DistanceUnitUtil;

import java.util.ArrayList;
import java.util.List;

public class MatrixRequestHandler {
    public static MatrixResult generateRouteFromRequest(MatrixRequest request) throws StatusCodeException {
        heigit.ors.matrix.MatrixRequest matrixRequest = convertMatrixRequest(request);
        try {
            return RoutingProfileManager.getInstance().computeMatrix(matrixRequest);
        } catch (Exception e) {
            if (e instanceof StatusCodeException)
                throw (StatusCodeException) e;

            throw new StatusCodeException(MatrixErrorCodes.UNKNOWN);
        }
    }

    private static heigit.ors.matrix.MatrixRequest convertMatrixRequest(MatrixRequest request) throws StatusCodeException {
        //TODO Next
        heigit.ors.matrix.MatrixRequest matrixRequest = new heigit.ors.matrix.MatrixRequest();
        matrixRequest.setProfileType(convertMatrixProfileType(request.getProfile()));
        Coordinate[] locations = convertLocations(request.getLocations());
        if (request.isHasId())
            matrixRequest.setId(request.getId());
        if (!request.hasValidSourceIndex())
            throw new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "sources");
        matrixRequest.setSources(convertSources(request.getSources(), locations));
        if (!request.hasValidDestinationIndex())
            throw new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "destinations");
        matrixRequest.setDestinations(convertDestinations(request.getSources(), locations));
        if (request.hasMetrics()) {
            matrixRequest.setMetrics(request.getMetrics());
        }
        if (request.hasUnits()) {
            matrixRequest.setUnits(convertUnits(request.getUnits()));
        }
        return matrixRequest;
    }

    private static Coordinate[] convertLocations(List<List<Double>> locations) throws ParameterValueException {
        if (locations.size() < 2)
            throw new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "locations");

        ArrayList<Coordinate> locationCoordinates = new ArrayList<>();

        for (List<Double> coordinate : locations) {
            locationCoordinates.add(convertSingleLocationCoordinate(coordinate));
        }
        try {
            return locationCoordinates.toArray(new Coordinate[locations.size()]);
        } catch (NumberFormatException | ArrayStoreException | NullPointerException ex) {
            throw new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "locations");
        }
    }

    private static Coordinate convertSingleLocationCoordinate(List<Double> coordinate) throws ParameterValueException {
        if (coordinate.size() != 2)
            throw new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "locations");
        return new Coordinate(coordinate.get(0), coordinate.get(1));
    }

    private static Coordinate[] convertSources(String[] sourcesIndex, Coordinate[] locations) throws ParameterValueException {
        int length = sourcesIndex.length;
        if (length == 0) return locations;
        if (length == 1 && "all".equalsIgnoreCase(sourcesIndex[0])) return locations;
        try {
            ArrayList<Coordinate> indexCoordinateArray = convertIndexToLocations(sourcesIndex, locations);
            return indexCoordinateArray.toArray(new Coordinate[indexCoordinateArray.size()]);
        } catch (NumberFormatException | ArrayStoreException | NullPointerException ex) {
            throw new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "sources");
        }
    }

    private static Coordinate[] convertDestinations(String[] destinationsIndex, Coordinate[] locations) throws ParameterValueException {
        // TODO Check for: all, sources >= locations.index >> break,
        int length = destinationsIndex.length;
        if (length == 0) return locations;
        if (length == 1 && "all".equalsIgnoreCase(destinationsIndex[0])) return locations;
        try {
            ArrayList<Coordinate> indexCoordinateArray = convertIndexToLocations(destinationsIndex, locations);
            return indexCoordinateArray.toArray(new Coordinate[indexCoordinateArray.size()]);
        } catch (NumberFormatException | ArrayStoreException | NullPointerException ex) {
            throw new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "destinations");
        }
    }

    private static ArrayList<Coordinate> convertIndexToLocations(String[] index, Coordinate[] locations) throws NumberFormatException {
        ArrayList<Coordinate> indexCoordinates = new ArrayList<>();
        for (String indexString : index) {
            try {
                int indexInteger = Integer.parseInt(indexString);
                indexCoordinates.add(locations[indexInteger]);
            } catch (NumberFormatException ex) {
                throw new NumberFormatException();
            }
        }
        return indexCoordinates;
    }

    private static DistanceUnit convertUnits(APIEnums.Units unitsIn) throws ParameterValueException {
        DistanceUnit units = DistanceUnitUtil.getFromString(unitsIn.toString(), DistanceUnit.Unknown);
        if (units == DistanceUnit.Unknown)
            throw new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "units", unitsIn.toString());

        return units;
    }

    private static int convertMatrixProfileType(APIEnums.MatrixProfile profile) throws ParameterValueException {
        try {
            return RoutingProfileType.getFromString(profile.toString());
        } catch (Exception e) {
            throw new ParameterValueException(MatrixErrorCodes.INVALID_PARAMETER_VALUE, "profile");
        }
    }
}
