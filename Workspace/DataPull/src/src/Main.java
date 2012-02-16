package src;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sun.tools.java.Type;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerType;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
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
	@SuppressWarnings("null")
	private static ContentStructure store_vm_contents(VirtualMachine target_vm) {
		ContentStructure head = new ContentStructure("Base", null, "head", (long)0, new ArrayList<ContentStructure>(), target_vm);
		
		for (ThreadReference current_tr:target_vm.allThreads()) {
			current_tr.suspend();
			if (!(current_tr.name().equals("Reference Handler") || current_tr.name().equals("Finalizer") || current_tr.name().equals("Signal Dispatcher"))) {
				ContentStructure new_thread_cs = new ContentStructure(current_tr.name(), null, "thread", current_tr.uniqueID(), new ArrayList<ContentStructure>(), current_tr);				
				head.contents.add(new_thread_cs);
				// here we might want to say that new_thread_cs is pointed to BY head, like another array to hold that data
				
				int stack_number = 0;
				try {
					for (StackFrame current_stack:current_tr.frames()) {						
						StackStructure new_stack_cs = new StackStructure(current_stack.toString(), null, "stack_frame", (long)stack_number, new ArrayList<ContentStructure>(), current_stack, current_tr.frames().indexOf(current_stack));
						stack_number++;
						new_thread_cs.contents.add(new_stack_cs);
						try {
							for (LocalVariable current_localvar:current_stack.visibleVariables()) {
								Value v = current_stack.getValue(current_localvar);
								ObjectReference ob = null;
								//System.out.println(v);
								ob = ((ObjectReference) v);
								ReferenceType reft = ob.referenceType();
								System.out.println("referencet type="+reft);
								List<Field> farr = reft.allFields();
								
								if (v instanceof ObjectReference){
							
									//System.out.println(v.type().name() +" is an object");

									if (ob instanceof ArrayReference){
										//System.out.println("ARRRRR");
										System.out.println(current_localvar.name());
										System.out.println(((ArrayReference) ob).getValues());
										System.out.println("----");
									}
									if (ob instanceof StringReference){
										//System.out.println("ARRRRR");
										System.out.println(current_localvar.name());
										System.out.println(((StringReference) ob).value());
										System.out.println("----");
									}
									if (ob instanceof ClassObjectReference){
										//System.out.println("ARRRRR");
										System.out.println("OBREF");
										//System.out.println(k.name());
										//System.out.println(((ClassObjectReference) ob).reflectedType());
										//System.out.println("----");
									}
									if (ob instanceof ClassLoaderReference){
										//System.out.println("ARRRRR");
										System.out.println("LOADER");
										//System.out.println(k.name());
										//System.out.println(((ClassObjectReference) ob).reflectedType());
										//System.out.println("----");
									}
								}
								//System.out.println("THIS:" + ob.getClass());
								System.out.println();
								ContentStructure new_variable_cs = new ContentStructure(current_localvar.name(), current_stack.getValue(current_localvar).toString(), current_localvar.typeName(), (long)current_localvar.hashCode(), new ArrayList<ContentStructure>(), current_localvar);
								new_stack_cs.contents.add(new_variable_cs);
								System.out.println("THIS VARIABLE IS OF TYPE: " + current_stack.getValue(current_localvar).type().name());
								System.out.println();
								//System.out.println("        " + k.typeName() + " " + k.name() + " = " + j.getValue(k));
							}
						} catch (AbsentInformationException e) {
							
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
