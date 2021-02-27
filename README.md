# matsim-freight-analysis

matsim-freight-analysis provides basic functionality for generating basic statistics about vehicles, carriers, shipments and services in matsim runs.



## Collecting data
matsim-freight-analysis can be used during or after matsim runs.

### Running with the simulation

Add the EventHandler to your MATSim controller in your desired way. MATSim-freight-analyis will then by default collect data about all freight vehicles, carriers, shipments and services.


<a id="org911123b"></a>

### Running on simulation output
Create the EventHandler and use a MATSim Events Reader to read the Events file into it. MATSim-freight-analyis will then by default collect data about all freight vehicles, carriers, shipments and services.

#### Prerequisites:

1.  Events file
2.  allVehicles file
3.  Carrier file
4.  Network file

## Exporting data

To export the collected data, call one of the export methods provided by the EventsHandler:

    exportVehicleInfo(outputPath, exportGuesses)
    exportVehicleTripInfo(outputPath, exportGuesses)
    exportCarrierInfo(outputPath, exportGuesses)
    exportServiceInfo(outputPath, exportGuesses)
    exportShipmentInfo(outputPath, exportGuesses)

You can specify the output directory and whether the output should include guesses (see Limitations).


<a id="orga67d0e6"></a>

### Output format

Files are exported as .tsv (tab-separated-values). The first row contains a description of the columns contents. Carrier, shipment and service info are exported to single files for further analysis (e.g. with R) and to separate files grouped by carriers for fast manual evaluation.
Guesses are preceeded by "?". Data dependent from the guess (e.g. travel time of a guessed vehicle) are not preceeded by another "?", if they aren't a guess as well. 


<a id="org636dc44"></a>

## Limitations

Several Limitations still exist:

1.  Whether a vehicle is a freight vehicle is determined by the whether the vehicle Id contains the String "freight" or not. This works for default configurations of matsim-freight, but can break easily in different setups or future versions of MATSim freight.
2.  By now, all functionality matching objects from the jsprit side (carriers, shipments and services) to the MATSim side (vehicles or drivers) rely on educated guesses whose quality heavily depends on the individual scenario. The lesser correlation of time windows and links for shipments and services, the better the guesses will be.
3.  If you are using LSP type Events (see [GitHub](https://github.com/matsim-org/matsim-libs/tree/3bd8e6f6a227181ca382d690f5ff37cf0b4d9afa/contribs/freight/src/main/java/org/matsim/contrib/freight/events)), guesses will not be needed anymore, but the handling of those Events is merely based on theoretical evaluation of the structure of those events and therefore IS NOT TESTED IN ANY WAY and will most likely not work out of the box. If you have sample output of such a scenario, feel free to share it me so I can maintain the tool or do it yourself and open a pull request. As of 02/21 it is not clear whether matsim-freight will move in the direction of throwing those events or extending the activity events to resolve the need for guessing. If necessary, this tool will abandon the LSP Event Handling and move on to whichever direction is chosen by the matsim-freight maintainers.

