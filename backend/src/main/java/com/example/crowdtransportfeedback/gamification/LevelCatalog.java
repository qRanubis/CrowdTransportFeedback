package com.example.crowdtransportfeedback.gamification;
import java.util.List;
public final class LevelCatalog {
 public record Level(int level,int threshold,String title){}
 public static final List<Level> LEVELS=List.of(new Level(1,0,"Passenger"),new Level(2,100,"Contributor"),new Level(3,225,"Observer"),new Level(4,375,"Explorer"),new Level(5,550,"Route Explorer"),new Level(6,775,"Network Explorer"),new Level(7,1050,"Navigator"),new Level(8,1375,"City Navigator"),new Level(9,1750,"Transit Mapper"),new Level(10,2175,"Network Mapper"),new Level(11,2650,"Mobility Analyst"),new Level(12,3175,"Senior Mapper"),new Level(13,3750,"Transit Specialist"),new Level(14,4375,"Network Specialist"),new Level(15,5000,"Urban Mobility Expert"));
 private LevelCatalog(){} public static Level forXp(long xp){return LEVELS.stream().filter(l->xp>=l.threshold).reduce((a,b)->b).orElse(LEVELS.getFirst());}
 public static Integer nextThreshold(long xp){var l=forXp(xp);return l.level==15?null:LEVELS.get(l.level).threshold;}
}
