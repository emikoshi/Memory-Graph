import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
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

import javax.swing.JFrame;
public class Main {
	
	// java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n <Classname>
	
	public static void main (String args[]) {
		// get the virtual machine from create_vm with the specified parameters
		VirtualMachine target_vm = create_vm("com.sun.jdi.SocketAttach", 8000);
		
		// do stuff with the vm
		print_vm_contents(target_vm);
		System.out.println("\nStarting to store contents...");
		ContentStructure cs = store_vm_contents(target_vm);
		
		System.out.println("\nPrinting the ContentStructure we gathered from the VM...");
		
		print_content_structure(cs,"");
		
		Grapher frame = new Grapher(cs);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1000, 720);
		frame.setVisible(true);
        //frame.setEnabled(false);
	}

	private static VirtualMachine create_vm(String connectorName, int port) {
		// first, get the virtual machine
		
		// first thing we must do is create the Bootstrap virtual machine manager
		// this will allow us to connect to the virtual machine running the target program
		// and the debugger associated with the target program.
		VirtualMachineManager first = Bootstrap.virtualMachineManager();

		// create a list of AttachingConnectors to choose which one to connect to 
		List<AttachingConnector> attaching_connectors_list = first.attachingConnectors();
		List<Connector> connectors_list = first.allConnectors();
		
		// display the AttachingConnectors list
		System.out.println("attachingConnectors / their names");
		for (AttachingConnector i:attaching_connectors_list) System.out.println(i + " \n name: " + i.name());
		System.out.println();
		
		//System.out.println("allConnectors / their names");
		//for (Connector i:connectors_list) System.out.println(i + " \n name: " + i.name());
		//System.out.println();
		
		
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
					System.out.println(default_connector_map);
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
	private static void print_vm_contents(VirtualMachine target_vm) {
		// TODO Auto-generated method stub
		System.out.println("Now printing all threads..");
		for (ThreadReference i:target_vm.allThreads()) {
			i.suspend();			
			if (!(i.name().equals("Reference Handler") || i.name().equals("Finalizer") || i.name().equals("Signal Dispatcher"))) {
				System.out.println(i);
				try {
					for (StackFrame j:i.frames()) {
						System.out.println("    " +j.location());
						try {
							for (LocalVariable k:j.visibleVariables()) {
								System.out.println("        " + k.typeName() + " " + k.name() + " = " + j.getValue(k));
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
			} else {
				System.out.println("ignoring basic java thread: " + i);
			}
		}
	}
	
	/* ContentStructure method 
	 * this method returns a ContentStructure containing all of the threads, stacks, and variables within this program
	 * possible problems: infinite loop if there is a cycle in the graph (a sequence of nodes that point to each other).
	 */
	private static ContentStructure store_vm_contents(VirtualMachine target_vm) {
		ContentStructure head = new ContentStructure("Base", null, "head", (long)0, new ArrayList<ContentStructure>(), target_vm);
		
		for (ThreadReference i:target_vm.allThreads()) {
			i.suspend();
			if (!(i.name().equals("Reference Handler") || i.name().equals("Finalizer") || i.name().equals("Signal Dispatcher"))) {
				ContentStructure new_thread_cs = new ContentStructure(i.name(), null, "thread", i.uniqueID(), new ArrayList<ContentStructure>(), i);				
				head.contents.add(new_thread_cs);
				// here we might want to say that new_thread_cs is pointed to BY head, like another array to hold that data
				
				int stack_number = 0;
				try {
					for (StackFrame j:i.frames()) {						
						ContentStructure new_stack_cs = new ContentStructure(j.toString(), null, "stack_frame", (long)stack_number, new ArrayList<ContentStructure>(), j);
						stack_number++;
						new_thread_cs.contents.add(new_stack_cs);
						try {
							for (LocalVariable k:j.visibleVariables()) {
								ContentStructure new_variable_cs = new ContentStructure(k.name(), j.getValue(k).toString(), k.typeName(), (long)k.hashCode(), new ArrayList<ContentStructure>(), k);
								new_stack_cs.contents.add(new_variable_cs);
								System.out.println("THIS VARIABLE IS OF TYPE: " + j.getValue(k).type().name());
								//System.out.println("        " + k.typeName() + " " + k.name() + " = " + j.getValue(k));
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
		return head;		
	}
	
	private static void print_content_structure(ContentStructure cs, String spaces) {
		System.out.println(spaces + cs.type + " " + cs.name + (cs.value == null ? "" : " = " + cs.value));	
		for (ContentStructure i:cs.contents) print_content_structure(i, spaces+"   ");		
	}	
}