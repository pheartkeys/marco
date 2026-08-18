package com.example.ui.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class AirportItem(
    val code: String,
    val city: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class MotivationItem(
    val id: String,
    val label: String,
    val description: String,
    val sampleDestination: String,
    val sampleCountryCode: String,
    val durationDays: Int,
    val estimatedBudget: Double
)

data class LoyaltyProgramCatalogItem(
    val id: String,
    val name: String,
    val categoryType: String, // AIRLINE, HOTEL, TIMESHARE, CREDIT_CARD
    val tierOptions: List<String>,
    val defaultUnit: String
)

/**
 * A program the user linked in the onboarding wizard, with everything they actually told Marco
 * about it: tier and balance are genuinely optional and stay blank if skipped. Booking rank is
 * ordinal, not a weight (someone either books Delta before Alaska or they don't), and is
 * represented purely by this item's position within its category in the wizard's selection list
 * — there is no separate rank field to fall out of sync with it.
 */
data class SelectedLoyaltyProgram(
    val program: LoyaltyProgramCatalogItem,
    val tier: String = "",
    val balance: String = ""
)

// Which UserPreferenceEntity field a comfort/accessibility option's free-text notes should be
// routed into. Explicit routing avoids re-deriving intent from label substrings, which silently
// drops any option whose label doesn't match one of the hardcoded patterns.
enum class ComfortTarget { WHEELCHAIR, SENSORY, DIETARY, MOBILITY }

data class ComfortOption(
    val label: String,
    val target: ComfortTarget
)

object PreferenceConstants {

    val MAJOR_AIRPORTS = listOf(
        AirportItem("JFK", "New York", "John F. Kennedy International", 40.6413, -73.7781),
        AirportItem("EWR", "Newark / New York", "Newark Liberty International", 40.6895, -74.1745),
        AirportItem("LGA", "New York", "LaGuardia Airport", 40.7769, -73.8740),
        AirportItem("LAX", "Los Angeles", "Los Angeles International", 33.9416, -118.4085),
        AirportItem("ORD", "Chicago", "O'Hare International", 41.9742, -87.9073),
        AirportItem("DFW", "Dallas / Fort Worth", "Dallas/Fort Worth International", 32.8998, -97.0403),
        AirportItem("DEN", "Denver", "Denver International", 39.8561, -104.6737),
        AirportItem("ATL", "Atlanta", "Hartsfield-Jackson Atlanta International", 33.6407, -84.4277),
        AirportItem("SFO", "San Francisco", "San Francisco International", 37.6213, -122.3790),
        AirportItem("SEA", "Seattle", "Seattle-Tacoma International", 47.4502, -122.3088),
        AirportItem("MIA", "Miami", "Miami International", 25.7959, -80.2870),
        AirportItem("BOS", "Boston", "Boston Logan International", 42.3656, -71.0096),
        AirportItem("IAD", "Washington, D.C.", "Washington Dulles International", 38.9531, -77.4565),
        AirportItem("PHX", "Phoenix", "Phoenix Sky Harbor International", 33.4373, -112.0078),
        AirportItem("MSP", "Minneapolis / St. Paul", "Minneapolis-Saint Paul International", 44.8848, -93.2223),
        AirportItem("DTW", "Detroit", "Detroit Metropolitan", 42.2162, -83.3554),
        AirportItem("PHL", "Philadelphia", "Philadelphia International", 39.8729, -75.2437),
        AirportItem("CLT", "Charlotte", "Charlotte Douglas International", 35.2144, -80.9473),
        AirportItem("SAN", "San Diego", "San Diego International", 32.7338, -117.1933),
        AirportItem("TPA", "Tampa", "Tampa International", 27.9772, -82.5311),
        AirportItem("PDX", "Portland", "Portland International", 45.5898, -122.5951),
        AirportItem("BNA", "Nashville", "Nashville International", 36.1263, -86.6774),
        AirportItem("AUS", "Austin", "Austin-Bergstrom International", 30.1975, -97.6664),
        AirportItem("RDU", "Raleigh / Durham", "Raleigh-Durham International", 35.8801, -78.7880),
        AirportItem("STL", "St. Louis", "St. Louis Lambert International", 38.7472, -90.3599),
        AirportItem("HNL", "Honolulu", "Daniel K. Inouye International", 21.3245, -157.9251),
        AirportItem("OGG", "Maui / Kahului", "Kahului Airport", 20.8986, -156.4305),
        AirportItem("LHR", "London", "Heathrow Airport", 51.4700, -0.4543),
        AirportItem("CDG", "Paris", "Charles de Gaulle Airport", 49.0097, 2.5479),
        AirportItem("HND", "Tokyo", "Tokyo Haneda Airport", 35.5494, 139.7798),
        AirportItem("SIN", "Singapore", "Singapore Changi Airport", 1.3644, 103.9915),
        AirportItem("SYD", "Sydney", "Sydney Kingsford Smith Airport", -33.9399, 151.1753)
    )

    fun findNearestAirport(latitude: Double, longitude: Double): AirportItem {
        return MAJOR_AIRPORTS.minByOrNull { airport ->
            val dLat = Math.toRadians(airport.latitude - latitude)
            val dLon = Math.toRadians(airport.longitude - longitude)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(latitude)) * cos(Math.toRadians(airport.latitude)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            c
        } ?: MAJOR_AIRPORTS.first()
    }

    val MOTIVATIONS = listOf(
        MotivationItem(
            id = "Escape",
            label = "Escape",
            description = "Unplug, recharge, and leave routine behind",
            sampleDestination = "Kauai, Hawaii",
            sampleCountryCode = "US",
            durationDays = 6,
            estimatedBudget = 3200.0
        ),
        MotivationItem(
            id = "Milestone",
            label = "Milestone",
            description = "Celebrate an anniversary, birthday, or achievement",
            sampleDestination = "Amalfi Coast, Italy",
            sampleCountryCode = "IT",
            durationDays = 7,
            estimatedBudget = 4800.0
        ),
        MotivationItem(
            id = "Reconnect",
            label = "Reconnect",
            description = "Quality time with loved ones and friends",
            sampleDestination = "Lake Tahoe, California",
            sampleCountryCode = "US",
            durationDays = 5,
            estimatedBudget = 2600.0
        ),
        MotivationItem(
            id = "Work",
            label = "Work",
            description = "Bleisure, deep focus, and corporate retreats",
            sampleDestination = "Tokyo, Japan",
            sampleCountryCode = "JP",
            durationDays = 5,
            estimatedBudget = 3500.0
        ),
        MotivationItem(
            id = "Explore",
            label = "Explore",
            description = "Wanderlust, uncharted culture, and active exploration",
            sampleDestination = "Kyoto & Swiss Alps",
            sampleCountryCode = "CH",
            durationDays = 8,
            estimatedBudget = 4200.0
        )
    )

    val LOYALTY_CATALOG = listOf(
        // Airlines
        LoyaltyProgramCatalogItem("delta", "Delta SkyMiles", "AIRLINE", listOf("General Member", "Silver Medallion", "Gold Medallion", "Platinum Medallion", "Diamond Medallion"), "Miles"),
        LoyaltyProgramCatalogItem("united", "United MileagePlus", "AIRLINE", listOf("Member", "Premier Silver", "Premier Gold", "Premier Platinum", "Premier 1K"), "Miles"),
        LoyaltyProgramCatalogItem("aa", "American AAdvantage", "AIRLINE", listOf("Member", "AAdvantage Gold", "AAdvantage Platinum", "Platinum Pro", "Executive Platinum"), "Miles"),
        LoyaltyProgramCatalogItem("southwest", "Southwest Rapid Rewards", "AIRLINE", listOf("Standard", "A-List", "A-List Preferred", "Companion Pass"), "Points"),
        LoyaltyProgramCatalogItem("alaska", "Alaska Mileage Plan", "AIRLINE", listOf("Member", "MVP", "MVP Gold", "MVP Gold 75K", "MVP Gold 100K"), "Miles"),
        LoyaltyProgramCatalogItem("flying_blue", "Air France / KLM Flying Blue", "AIRLINE", listOf("Explorer", "Silver", "Gold", "Platinum"), "Miles"),
        LoyaltyProgramCatalogItem("ba", "British Airways Executive Club", "AIRLINE", listOf("Blue", "Bronze", "Silver", "Gold"), "Avios"),

        // Hotels
        LoyaltyProgramCatalogItem("marriott", "Marriott Bonvoy", "HOTEL", listOf("Member", "Silver Elite", "Gold Elite", "Platinum Elite", "Titanium Elite", "Ambassador"), "Points"),
        LoyaltyProgramCatalogItem("hyatt", "World of Hyatt", "HOTEL", listOf("Member", "Discoverist", "Explorist", "Globalist"), "Points"),
        LoyaltyProgramCatalogItem("hilton", "Hilton Honors", "HOTEL", listOf("Member", "Silver", "Gold", "Diamond"), "Points"),
        LoyaltyProgramCatalogItem("ihg", "IHG One Rewards", "HOTEL", listOf("Club", "Silver Elite", "Gold Elite", "Platinum Elite", "Diamond Elite"), "Points"),

        // Timeshares & Vacation Clubs
        LoyaltyProgramCatalogItem("dvc", "Disney Vacation Club (DVC)", "TIMESHARE", listOf("Owner (Direct)", "Owner (Resale)"), "Vacation Points"),
        LoyaltyProgramCatalogItem("mvc", "Marriott Vacation Club", "TIMESHARE", listOf("Owner", "Select", "Executive", "Presidential", "Chairman"), "Club Points"),
        LoyaltyProgramCatalogItem("rci", "RCI Points / Weeks", "TIMESHARE", listOf("Subscribed Member", "Platinum Member"), "TPU Points"),
        LoyaltyProgramCatalogItem("ii", "Interval International", "TIMESHARE", listOf("Basic Member", "Gold Member", "Platinum Member"), "Points"),
        LoyaltyProgramCatalogItem("hgv", "Hilton Grand Vacations", "TIMESHARE", listOf("Owner", "Elite", "Elite Plus", "Elite Premier"), "Club Points"),

        // Credit Cards & Flexible Currencies
        LoyaltyProgramCatalogItem("chase", "Chase Ultimate Rewards", "CREDIT_CARD", listOf("Sapphire Preferred", "Sapphire Reserve", "Ink Preferred"), "Points"),
        LoyaltyProgramCatalogItem("amex", "American Express Membership Rewards", "CREDIT_CARD", listOf("Gold Card", "Platinum Card", "Business Platinum"), "Points"),
        LoyaltyProgramCatalogItem("capital_one", "Capital One Miles", "CREDIT_CARD", listOf("Venture Rewards", "Venture X"), "Miles"),
        LoyaltyProgramCatalogItem("citi", "Citi ThankYou Rewards", "CREDIT_CARD", listOf("Premier", "Prestige", "Strata Premier"), "Points")
    )

    val PACING_OPTIONS = listOf(
        "Slow & Immersive" to "Unrushed mornings, 1-2 curated activities max per day, spontaneous discovery",
        "Morning Active / Relaxed Afternoon" to "Active excursions before noon, leisurely afternoons and fine dining",
        "High-Energy Full Day" to "Sunrise to sunset exploration, immersive adventures and cultural highlights"
    )

    // Restored as its own weighted question — was cut and its field got hijacked by motivation.
    val TRAVEL_STYLES = listOf(
        "Balanced Scenic Explorer" to "Cultural discovery with scenic vistas and balanced walking pace.",
        "Slow-Paced Relaxation" to "Leisurely mornings, long culinary lunches, and spa retreats.",
        "High-Intensity Cultural Trail" to "Full-day historical itineraries, sunrise photography, and multi-site tours.",
        "Luxury Multi-Gen Villa Escape" to "Private transport, multi-bedroom timeshares, and kid-friendly adventures."
    )

    val ACCESSIBILITY_COMFORT_OPTIONS = listOf(
        ComfortOption("Step-Free & Wheelchair Routing", ComfortTarget.WHEELCHAIR),
        ComfortOption("Sensory-Quiet & Low-Stimulation Venues", ComfortTarget.SENSORY),
        ComfortOption("Elevator & Priority Boarding Access", ComfortTarget.MOBILITY),
        ComfortOption("Dietary Allergy-Aware Dining (Gluten/Nut/Dairy)", ComfortTarget.DIETARY)
    )

    // Track C: Taxonomy Additions
    val TRANSPORT_TAXONOMY = listOf(
        TaxonomyItem("bicycle", "Bicycle & Micro-Mobility", "Eco-friendly, urban bike paths, e-bikes"),
        TaxonomyItem("rail", "High-Speed & Regional Rail", "Scenic trains, intercity express, sleeper cabins"),
        TaxonomyItem("economy_flight", "Commercial Economy Flight", "Standard seating, mainline carriers"),
        TaxonomyItem("business_flight", "Premium & Business Class Flight", "Lie-flat suites, airport lounge access"),
        TaxonomyItem("charter", "Private Charter Flight", "Flexible regional group charters"),
        TaxonomyItem("private_jet", "Private Jet Aviation", "Ultra-long range executive private aircraft")
    )

    val LODGING_TAXONOMY = listOf(
        TaxonomyItem("hostel", "Boutique Hostel & Co-Living", "Shared social spaces, private pod rooms"),
        TaxonomyItem("timeshare", "Vacation Club & Timeshare Suite", "Multi-bedroom condos with kitchens"),
        TaxonomyItem("boutique_hotel", "Boutique Heritage Hotel", "Curated design, local culture, personalized service"),
        TaxonomyItem("luxury_resort", "5-Star Luxury Resort", "Full-service spa, beachfront, fine dining amenities"),
        TaxonomyItem("private_villa", "Private Estate & Villa", "Exclusive buyout, private pool, dedicated concierge")
    )

    val DINING_TAXONOMY = listOf(
        TaxonomyItem("street_food", "Authentic Street Food & Night Markets", "Local stalls, culinary heritage, food walks"),
        TaxonomyItem("casual_dining", "Casual Neighborhood Bistro", "Warm ambiance, regional farm-to-table menus"),
        TaxonomyItem("notable_bistro", "Notable Michelin Bib Gourmand", "Celebrated chefs, creative regional cuisine"),
        TaxonomyItem("fine_dining", "Michelin-Starred Fine Dining", "Multi-course tasting menus, wine pairing")
    )
}

data class TaxonomyItem(
    val id: String,
    val label: String,
    val description: String
)

