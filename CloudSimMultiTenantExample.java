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
 * A simple example showing how to create a multi-tenant data center
 * where each tenant has different VMs and cloudlets.
 */
public class CloudSimMultiTenantExample {
    public static List<DatacenterBroker> brokers = new ArrayList<>();
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;

    public static void main(String[] args) {
        Log.println("Starting CloudSimMultiTenantExample...");

        try {
            // Initialize CloudSim package
            int num_user = 3; // Multiple cloud users (tenants)
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            // Create a data center
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            // Create brokers for each tenant
            for (int i = 0; i < num_user; i++) {
                DatacenterBroker broker = new DatacenterBroker("Broker_" + i);
                brokers.add(broker);
            }

            // Create VMs and Cloudlets for each broker (tenant)
            vmlist = new ArrayList<>();
            cloudletList = new ArrayList<>();

            for (int i = 0; i < num_user; i++) {
                DatacenterBroker broker = brokers.get(i);
                createVMsAndCloudlets(broker, i); // Create VMs and Cloudlets for each tenant
                broker.submitGuestList(vmlist); // Submit VM list
                broker.submitCloudletList(cloudletList); // Submit Cloudlet list
            }

            // Start the simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // Print the results
            for (DatacenterBroker broker : brokers) {
                List<Cloudlet> newList = broker.getCloudletReceivedList();
                printCloudletList(newList);
            }

            Log.println("CloudSimMultiTenantExample finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.println("Unwanted errors happen");
        }
    }

    /**
     * Creates the datacenter.
     *
     * @param name the name
     * @return the datacenter
     */
    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();
        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // Add a single PE

        int hostId = 0;
        int ram = 2048; // host memory
        long storage = 1000000; // host storage
        int bw = 10000; // bandwidth

        hostList.add(new Host(
            hostId,
            new RamProvisionerSimple(ram),
            new BwProvisionerSimple(bw),
            storage,
            peList,
            new VmSchedulerTimeShared(peList)
        ));

        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, time_zone, cost, costPerMem,
            costPerStorage, costPerBw
        );

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    /**
     * Creates VMs and Cloudlets for each tenant.
     *
     * @param broker the broker for the tenant
     * @param tenantId the tenant's id
     */
    private static void createVMsAndCloudlets(DatacenterBroker broker, int tenantId) {
        // Create VMs for this tenant
        int vmid = 0;
        int mips = 1000;
        long size = 10000; // image size (MB)
        int ram = 512; // memory (MB)
        long bw = 1000;
        int pesNumber = 1; // number of CPUs
        String vmm = "Xen"; // Virtual Machine Manager

        Vm vm = new Vm(vmid, broker.getId(), mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
        vmlist.add(vm);

        // Create Cloudlets for this tenant
        int cloudletId = tenantId * 10;
        long length = 400000;
        long fileSize = 300;
        long outputSize = 300;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet cloudlet = new Cloudlet(cloudletId, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
        cloudlet.setUserId(broker.getId());
        cloudlet.setGuestId(vmid);

        cloudletList.add(cloudlet);
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
