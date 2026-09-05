package com.example.crowdtransportfeedback.analytics;
import static org.junit.jupiter.api.Assertions.*; import org.junit.jupiter.api.Test;
class GeoGridTest {
 @Test void assignmentIsDeterministicAndRoundTrips(){var a=GeoGrid.cell(44.4268,26.1025);assertEquals(a,GeoGrid.cell(44.4268,26.1025));assertEquals(a.id(),GeoGrid.fromId(a.id()).id());}
 @Test void nearbyPointsShareCellButSeparatedPointsDoNot(){assertEquals(GeoGrid.cell(44.42680,26.10250).id(),GeoGrid.cell(44.42685,26.10255).id());assertNotEquals(GeoGrid.cell(44.4268,26.1025).id(),GeoGrid.cell(44.43,26.1025).id());}
 @Test void invalidCoordinatesAreRejected(){assertNull(GeoGrid.cell(91,0));assertNull(GeoGrid.cell(0,181));assertNull(GeoGrid.cell(Double.NaN,0));assertNull(GeoGrid.fromId("bad"));}
}
