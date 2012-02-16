import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.LongValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.VoidValue;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.ReferenceType;

public class Main {
	
	//Make sure to compile before, running the JVM
	//java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n <Classname>
	
	public static void main (String args[]) {
		//get the virtual machine from create_vm with the specified parameters
		VirtualMachine target_vm = create_vm("com.sun.jdi.SocketAttach", 8000);
	
		//Printing out the contents of the VM, however; is not needed this far into the project.
		//print_vm_contents(target_vm);
		System.out.println("\nStarting to store contents...");
		System.out.println("\nPrinting the ContentStructure we gathered from the VM...");
		
		ContentStructure cs = store_vm_contents(target_vm);
		print_content_structure(cs,"");
	}

	private static VirtualMachine create_vm(String connectorName, int port) {
		//Grab the virtual machine and create the Bootstrap virtual machine manager.
		//It will allow us to connect to the virtual machine running the target program
		//and the debugger associated with the target program.
		VirtualMachineManager CreateBoot = Bootstrap.virtualMachineManager();

		//Create a list of AttachingConnectors to choose which one to connect to 
		List<AttachingConnector> attaching_connectors_list = CreateBoot.attachingConnectors();
		
		//Commented this out because it was purely used for debugging. Its purpose
		//was to display the AttachingConnectors list to see what connector we should use.
		//List<Connector> connectors_list = CreateBoot.allConnectors();
		//System.out.println("attachingConnectors / their names");
		//for (AttachingConnector i:attaching_connectors_list) System.out.println(i + " \n name: " + i.name());
		//System.out.println();
		//System.out.println("allConnectors / their names");
		//for (Connector i:connectors_list) System.out.println(i + " \n name: " + i.name());
		//System.out.println();
		
		
		//This is used in order to attach to the SocketAttach Connector
		for (AttachingConnector i:attaching_connectors_list) {
			//If the current AttachingConnector i has the name "com.sun.jdi.SocketAttach"
			if (i.name().equals(connectorName)) {
				//Output to confirm, used for debugging
				//System.out.println("Attaching to... " + i.name());
			
				//Set a map default_connector_map equal to the default Connector map, output the values.
				//Again used for debugging.
				java.util.Map<String, Connector.Argument> default_connector_map = i.defaultArguments();
				
				//Printed out the connector map's default values, used for debugging.
				//System.out.println(default_connector_map.values());

				//Setting the port to 8000, the same as is used in terminal to start up the JVM
				Connector.IntegerArgument arg = (Connector.IntegerArgument)default_connector_map.get("port");
				arg.setValue(port);
				
				try {
					//Used to print the new connector map of SocketAttach, purely debugging.
					//System.out.println(default_connector_map);
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
	
//Commented the print of this function out in main because it was used to get the raw values
//of the VM, but really only needed for debugging.
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
	
	//This method returns a ContentStructure containing all of the threads, stacks, and variables within this program
	//Possible problems: infinite loop if there is a cycle in the graph (a sequence of nodes that point to each other).
	private static ContentStructure store_vm_contents(VirtualMachine target_vm) {
		ContentStructure head = new ContentStructure("Base", null, "head", (long)0, new ArrayList<ContentStructure>(), target_vm);
		
		for (ThreadReference i:target_vm.allThreads()) {
			i.suspend();
			if (!(i.name().equals("Reference Handler") || i.name().equals("Finalizer") || i.name().equals("Signal Dispatcher"))) {
				ContentStructure new_thread_cs = new ContentStructure(i.name(), null, "thread", i.uniqueID(), new ArrayList<ContentStructure>(), i);				
				head.contents.add(new_thread_cs);
				//Here we might want to say that new_thread_cs is pointed to BY head, like another array to hold that data
				
				int stack_number = 0;
				try {
					for (StackFrame j:i.frames()) {						
						ContentStructure new_stack_cs = new ContentStructure(j.toString(), null, "stack_frame", (long)stack_number, new ArrayList<ContentStructure>(), j);
						stack_number++;
						new_thread_cs.contents.add(new_stack_cs);
						try {
							for (LocalVariable k:j.visibleVariables()) {
								Value v = j.getValue(k);
								if (v instanceof PrimitiveValue){
									PrimitiveValue pv = null;
									pv = ((PrimitiveValue) v);
									if (pv instanceof BooleanValue){
										System.out.println(k.name()+" is a PrimitiveValue. Its type is BooleanValue, with a value of: "+pv.booleanValue());
									}
									if (pv instanceof ByteValue){
										System.out.println(k.name()+" is a PrimitiveValue. Its type is ByteValue, with a value of: "+pv.byteValue());
									}
									if (pv instanceof CharValue){
										System.out.println(k.name()+" is a PrimitiveValue. Its type is CharValue, with a value of: "+pv.charValue());
									}
									if (pv instanceof DoubleValue){
										System.out.println(k.name()+" is a PrimitiveValue. Its type is DoubleValue, with a value of: "+pv.doubleValue());
									}
									if (pv instanceof FloatValue){
										System.out.println(k.name()+" is a PrimitiveValue. Its type is FloatValue, with a value of: "+pv.floatValue());
									}
									if (pv instanceof IntegerValue){
										System.out.println(k.name()+" is a PrimitiveValue. Its type is IntegerValue, with a value of: "+pv.intValue());
									}
									if (pv instanceof LongValue){
										System.out.println(k.name()+" is a PrimitiveValue. Its type is LongValue, with a value of: "+pv.longValue());
									}
									if (pv instanceof ShortValue){
										System.out.println(k.name()+" is a PrimitiveValue. Its type is ShortValue, with a value of: "+pv.shortValue());
									}
									if (pv instanceof VoidValue){
										System.out.println(k.name()+" is a PrimitiveValue. Its type is VoidValue, and is...VOID");
									}	
								}
								
								if (v instanceof ObjectReference){
									System.out.println(((ObjectReference) v).uniqueID());
									ObjectReference ob = null;
									ob = ((ObjectReference) v);
									ReferenceType reft = ob.referenceType();
									List<Field> flist = reft.fields();
									System.out.println(k.name() + " is a reference of " + reft.name());
									System.out.println(k.name() + " = "+ob.getValues(flist));
									if (ob instanceof ArrayReference){
										List<Value> lolarr = ((ArrayReference) ob).getValues();
										Object[] listarr = lolarr.toArray();
										if (listarr.length < 1){
											System.out.println(((ArrayReference) ob).getValues());
										}
										else{
											for (int u=0; u<listarr.length; u++){
												System.out.println("Element " + u + " of "+k.name()+ " = "+ listarr[u]);
											}
										}
									}
									if (ob instanceof StringReference){
										System.out.println(((StringReference) ob).value());
									}
									if (ob instanceof ClassObjectReference){
										System.out.println("OBREF");
										//System.out.println(k.name());
										//System.out.println(((ClassObjectReference) ob).reflectedType());
										//System.out.println("----");
									}
									if (ob instanceof ClassLoaderReference){
										System.out.println("LOADER");
										//System.out.println(k.name());
										//System.out.println(((ClassObjectReference) ob).reflectedType());
										//System.out.println("----");
									}
								}
								System.out.println();
								ContentStructure new_variable_cs = new ContentStructure(k.name(), j.getValue(k).toString(), k.typeName(), (long)k.hashCode(), new ArrayList<ContentStructure>(), k);
								new_stack_cs.contents.add(new_variable_cs);
								//System.out.println("THIS VARIABLE IS OF TYPE: " + j.getValue(k).type().name());
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