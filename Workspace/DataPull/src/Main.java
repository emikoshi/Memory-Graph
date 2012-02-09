import java.io.IOException;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.ReferenceType;
import com.sun.xml.internal.xsom.impl.scd.Iterators.Map;

public class Main {

	public static void main (String args[]) {
		// get the virtual machine from create_vm with the specified parameters
		VirtualMachine target_vm = create_vm("com.sun.jdi.SocketAttach", 8000);
		
		// do stuff with the vm
		handle_vm(target_vm);
	}

	private static VirtualMachine create_vm(String connectorName, int port) {
		// first, get the virtual machine
		
		// first thing we must do is create the Bootstrap virtual machine manager
		// this will allow us to connect to the virtual machine running the target program
		// and the debugger associated with the target program.
		VirtualMachineManager first = Bootstrap.virtualMachineManager();

		// create a list of AttachingConnectors to choose which one to connect to 
		List<AttachingConnector> attaching_connectors_list = first.attachingConnectors();
		//List<Connector> connectors_list = first.allConnectors();
		
		// display the AttachingConnectors list
		//System.out.println("attachingConnectors / their names");
		//for (AttachingConnector i:attaching_connectors_list) System.out.println(i + " \n name: " + i.name());
		//System.out.println();
		
//		System.out.println("allConnectors / their names");
//		for (Connector i:connectors_list) System.out.println(i + " \n name: " + i.name());
//		System.out.println();
		
		
		// let's assume we want to attach to the SocketAttach connector ... 
		for (AttachingConnector i:attaching_connectors_list) {
			// if the current AttachingConnector i has the name "com.sun.jdi.SocketAttach"
			if (i.name().equals(connectorName)) {
				// output to confirm
				System.out.println("Attaching to... " + i.name());
			
				// set a map default_connector_map equal to the default Connector map, output the values
				java.util.Map<String, Connector.Argument> default_connector_map = i.defaultArguments();
				System.out.println(default_connector_map.values());

				// set the port to 8000 (we should avoid using magic numbers in the future)
				Connector.IntegerArgument arg = (Connector.IntegerArgument)default_connector_map.get("port");
				arg.setValue(port);
				
				try {					
					// when we've finished this part, a virtual machine is returned 
					return i.attach(default_connector_map);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalConnectorArgumentsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	private static void handle_vm(VirtualMachine target_vm) {
		// TODO Auto-generated method stub
		System.out.println("Now printing all threads..");
		for (ThreadReference i:target_vm.allThreads()) {
			i.suspend();
			System.out.println(i);
			try {
				for (StackFrame j:i.frames()) {
					System.out.println(" " +j.location());
					try {
						for (LocalVariable k:j.visibleVariables()) {
							System.out.println("  " + k.name() + " " + k.typeName());
						}
					} catch (AbsentInformationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (IncompatibleThreadStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}