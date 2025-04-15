package org.cloudbus.cloudsim.examples;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * A simple example of CloudSim with Carbon Aware simulation.
 * The simulation includes energy consumption and carbon emissions calculations.
 */
public class CarbonAwareCloudSimExample {
	public static DatacenterBroker broker;

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;
	/** The vmlist. */
	private static List<Vm> vmlist;

	/** Carbon emission factor (grams per kWh) */
	private static final double CARBON_EMISSION_FACTOR = 0.4; // in gCO2 per kWh (example value)

	/**
	 * Creates main() to run this example.
	 *
	 * @param args the args
	 */
	public static void main(String[] args) {
		Log.println("Starting CarbonAwareCloudSimExample...");

		try {
			// First step: Initialize the CloudSim package. It should be called before creating any entities.
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance(); // Calendar whose fields have been initialized with the current date and time.
			boolean trace_flag = false; // trace events

			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			Datacenter datacenter0 = createDatacenter("Datacenter_0");

			// Third step: Create Broker
			broker = new DatacenterBroker("Broker");
			int brokerId = broker.getId();

			// Fourth step: Create one virtual machine
			vmlist = new ArrayList<>();

			// VM description
			int vmid = 0;
			int mips = 1000;
			long size = 10000; // image size (MB)
			int ram = 512; // vm memory (MB)
			long bw = 1000;
			int pesNumber = 1; // number of cpus
			String vmm = "Xen"; // VMM name

			// create VM
			Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

			// add the VM to the vmList
			vmlist.add(vm);

			// submit vm list to the broker
			broker.submitGuestList(vmlist);

			// Fifth step: Create one Cloudlet
			cloudletList = new ArrayList<>();

			// Cloudlet properties
			int id = 0;
			long length = 400000;
			long fileSize = 300;
			long outputSize = 300;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			Cloudlet cloudlet = new Cloudlet(id, length, pesNumber, fileSize,
										outputSize, utilizationModel, utilizationModel, 
										utilizationModel);
			cloudlet.setUserId(brokerId);
			cloudlet.setGuestId(vmid);

			// add the cloudlet to the list
			cloudletList.add(cloudlet);

			// submit cloudlet list to the broker
			broker.submitCloudletList(cloudletList);

			// Sixth step: Starts the simulation
			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			//Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			printCloudletList(newList);

			Log.println("CarbonAwareCloudSimExample finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.println("Unwanted errors happen");
		}
	}

	/**
	 * Creates the datacenter.
	 *
	 * @param name the name
	 *
	 * @return the datacenter
	 */
	private static Datacenter createDatacenter(String name) {

		// Here are the steps needed to create a Datacenter with energy consumption and carbon emissions awareness
		List<Host> hostList = new ArrayList<>();
		List<Pe> peList = new ArrayList<>();

		int mips = 1000;

		// Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

		int hostId = 0;
		int ram = 2048; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;

		// Adding host to host list
		hostList.add(
			new Host(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerSimple(bw),
				storage,
				peList,
				new VmSchedulerTimeShared(peList)
			)
		);

		// DatacenterCharacteristics definition with added energy consumption and cost parameters
		String arch = "x86"; 
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0; 
		double cost = 3.0;
		double costPerMem = 0.05; 
		double costPerStorage = 0.001;
		double costPerBw = 0.0;
		LinkedList<Storage> storageList = new LinkedList<>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		// Create the Datacenter
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Simulate carbon emission calculation based on energy consumed (example calculation)
		double totalEnergyConsumed = calculateEnergyConsumed(hostList);
		double carbonEmissions = calculateCarbonEmissions(totalEnergyConsumed);

		Log.println("Total energy consumed: " + totalEnergyConsumed + " kWh");
		Log.println("Carbon emissions: " + carbonEmissions + " grams of CO2");

		return datacenter;
	}

	/**
	 * Calculate the energy consumed by the datacenter's hosts.
	 *
	 * @param hostList the list of hosts in the datacenter
	 * @return the total energy consumed in kWh
	 */
	private static double calculateEnergyConsumed(List<Host> hostList) {
		double totalEnergy = 0.0;
		for (Host host : hostList) {
			// Energy consumption is roughly based on RAM and CPU utilization (simplified for this example)
			double energyPerHost = host.getRamProvisioner().getAvailableRam() * 0.001; // hypothetical energy calculation
			totalEnergy += energyPerHost;
		}
		return totalEnergy;
	}

	/**
	 * Calculate the carbon emissions based on the energy consumed.
	 *
	 * @param energyConsumed the energy consumed in kWh
	 * @return the carbon emissions in grams of CO2
	 */
	private static double calculateCarbonEmissions(double energyConsumed) {
		// Carbon emission factor is assumed as 0.4 grams per kWh (this value can change depending on region)
		return energyConsumed * CARBON_EMISSION_FACTOR;
	}

	/**
	 * Prints the Cloudlet objects.
	 *
	 * @param list list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.println();
		Log.println("========== OUTPUT ==========");
		Log.println("Cloudlet ID" + indent + "STATUS" + indent
				+ "Data center ID" + indent + "VM ID" + indent + "Time" + indent
				+ "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (Cloudlet value : list) {
			cloudlet = value;
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
				Log.print("SUCCESS");

				Log.println(indent + indent + cloudlet.getResourceId()
						+ indent + indent + indent + cloudlet.getGuestId()
						+ indent + indent
						+ dft.format(cloudlet.getActualCPUTime()) + indent
						+ indent + dft.format(cloudlet.getExecStartTime())
						+ indent + indent
						+ dft.format(cloudlet.getExecFinishTime()));
			}
		}
	}
}