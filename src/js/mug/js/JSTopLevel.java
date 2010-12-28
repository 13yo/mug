package mug.js;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mug.Modules;

public class JSTopLevel {
	/*
	 * prototypes
	 */
	
	final JSObject objectPrototype = JSObject.createObjectPrototype();
	
	final JSObject functionPrototype = new JSObject(objectPrototype) { {
		// needed to reference self
		JSObject functionPrototype = this;

		set("apply", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSTopLevel.this, ths);
				return thsObj.invoke(l0, JSUtils.toJavaArray(JSUtils.asJSObject(JSTopLevel.this, l1)));
			}
		});
	} };
	
	{
		objectPrototype.set("valueOf", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return "[object Object]";
			}
		});
	}
	
	final JSObject arrayPrototype = new JSObject(objectPrototype) { {
		set("concat", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				// concatenate arrays to new array
				JSObject thsObj = JSUtils.asJSObject(JSTopLevel.this, ths);
				JSArray out = new JSArray(arrayPrototype, 0);
				Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				for (int j = 0, max = (int) JSUtils.asNumber(thsObj.get("length")); j < max; j++)
					out.push(thsObj.get(String.valueOf(j)));
				for (Object arg : arguments) {
					JSObject arr = JSUtils.asJSObject(JSTopLevel.this, arg);
					for (int j = 0, max = (int) JSUtils.asNumber(arr.get("length")); j < max; j++)
						out.push(arr.get(String.valueOf(j)));
				}
				return out;
			}
		});
		
		set("push", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				thsObj.set(String.valueOf((int) JSUtils.asNumber(thsObj.get("length"))), l0);
				return thsObj.get("length");
			}
		});
		
		set("pop", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				Object out = thsObj.get(String.valueOf(len-1));
				thsObj.set("length", len-1);
				return out;
			}
		});
		
		set("map", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSTopLevel.this, ths);
				JSObject func = JSUtils.asJSObject(JSTopLevel.this, l0);
				JSArray out = new JSArray(arrayPrototype, 0);
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				for (int i = 0; i < len; i++)
					out.push(func.invoke(ths, thsObj.get(String.valueOf(i)), i));
				return out;
			}
		});
		
		set("join", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSTopLevel.this, ths);
				StringBuffer sb = new StringBuffer();
				String delim = (l0 == null || l0.equals(JSNull.NULL)) ? "" : JSUtils.asString(l0); 
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				for (int i = 0; i < len; i++) {
					if (i > 0)
						sb.append(delim);
					sb.append(JSUtils.asString(thsObj.get(String.valueOf(i))));
				}
				return sb.toString();
			}
		});
		
		set("slice", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				JSObject thsObj = JSUtils.asJSObject(JSTopLevel.this, ths);
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				int start = (int) JSUtils.asNumber(l0);
				int end = l1 == null ? len : (int) JSUtils.asNumber(l1);
				if (end < 0)
					end += len + 1;
				JSArray out = new JSArray(arrayPrototype, 0);
				for (int i = start; i < end; i++)
					out.push(thsObj.get(String.valueOf(i)));
				return out;
			}
		});
		
		set("sort", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(final Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				// cast to strings
				Object[] arr = JSUtils.toJavaArray((JSObject) ths);
				for (int i = 0; i < arr.length; i++)
					arr[i] = JSUtils.asString(arr[i]);
				// sort
				final JSObject func = l0 != null ? (JSObject) l0 :
					new JSFunction(functionPrototype) {
						@Override
						public Object invoke(final Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
						{
							return ((JSString) l0).value.compareTo(((JSString) l1).value);
						}
					};
				Arrays.sort(arr, new Comparator<Object>() {
					@Override
					public int compare(Object a, Object b) {
						try {
							return (int) JSUtils.asNumber(func.invoke(ths, a, b));
						} catch (Exception e) {
							return 0;
						}
					}
				});
				// return new array
				JSArray out = new JSArray(getArrayPrototype(), 0);
				out.append(arr);
				return out;
			}
		});
	} };
	
	final JSObject stringPrototype = new JSObject(objectPrototype) { {
		set("valueOf", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				if (ths instanceof JSString)
					return ((JSString) ths).value;
				return ths.toString();
			}
		});
		
		set("charAt", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return String.valueOf(JSUtils.asString(ths).charAt((int) JSUtils.asNumber(l0)));
			}
		});
		
		set("charCodeAt", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return (double) JSUtils.asString(ths).charAt((int) JSUtils.asNumber(l0));
			}
		});
		
		set("replace", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				String value = JSUtils.asString(l1);
				if (l0 instanceof JSRegExp) {
					JSRegExp regexp = (JSRegExp) l0;
					Matcher matcher = regexp.getPattern().matcher(JSUtils.asString(ths));
					return regexp.isGlobal() ? matcher.replaceAll(value) : matcher.replaceFirst(value);
				} else {
					String match = JSUtils.asString(l0);
					return JSUtils.asString(ths).replaceFirst(Pattern.quote(match), value);
				}
			}
		});
		
		set("substring", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				String value = JSUtils.asString(ths);
				int start = (int) JSUtils.asNumber(l0);
				int end = l1 == null ? value.length() : (int) JSUtils.asNumber(l1);
				return value.substring(start, end);
			}
		});
		
		set("substr", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				String value = JSUtils.asString(ths);
				int start = (int) JSUtils.asNumber(l0);
				int end = l1 == null ? value.length() : ((int) JSUtils.asNumber(l1)) + start;
				return value.substring(start, end);
			}
		});
		
		set("indexOf", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				String value = JSUtils.asString(ths);
				String search = JSUtils.asString(l0);
				return value.indexOf(search);
			}
		});
		
		set("toLowerCase", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object index, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return JSUtils.asString(ths).toLowerCase();
			}
		});
		
		set("toUpperCase", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object index, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return JSUtils.asString(ths).toUpperCase();
			}
		});
		
		set("split", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Pattern pattern = (l0 instanceof JSRegExp) ? ((JSRegExp) l0).getPattern() : Pattern.compile(Pattern.quote(JSUtils.asString(l0)));
				String[] result = pattern.split(JSUtils.asString(ths));
				JSArray out = new JSArray(arrayPrototype, 0);
				for (int i = 0; i < result.length; i++)
					out.push(result[i]);
				return out;
			}
		});
		
		set("match", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				//[TODO] if l0 is string, compile
				Pattern pattern = ((JSRegExp) l0).getPattern();
				JSArray out = new JSArray(getArrayPrototype(), 0);
				Matcher matcher = pattern.matcher(JSUtils.asString(ths));
				if (!((JSRegExp) l0).isGlobal()) {
					if (!matcher.find())
						return JSNull.NULL;
					for (int i = 0; i < matcher.groupCount() + 1; i++)
						out.push(matcher.group(i));
				} else {
					while (matcher.find())
						out.push(matcher.group(0));
					if (out.getLength() == 0)
						return JSNull.NULL;
				}
				return out;
			}
		});
	} };
	
	final JSObject numberPrototype = new JSObject(objectPrototype) { {
		set("valueOf", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				if (ths instanceof JSNumber)
					return ((JSNumber) ths).value;
				return Double.NaN;
			}
		});

		set("toString", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				int base = (int) JSUtils.asNumber(l0);
				double value = JSUtils.asNumber(ths);
				switch (base) {
				case 2: return Integer.toBinaryString((int) value);
				case 8:	return Integer.toOctalString((int) value);
				case 16: return Integer.toHexString((int) value);
				}
				return Double.toString(value);
			}
		});
		
		set("toFixed", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				double value = JSUtils.asNumber(ths);
				int fixed = (int) JSUtils.asNumber(l0); 
				DecimalFormat formatter = new DecimalFormat("0" +
					(fixed > 0 ? "." + String.format(String.format("%%0%dd", fixed), 0) : ""));
				return formatter.format(value);
			}
		});
	} };
	
	final JSObject booleanPrototype = new JSObject(objectPrototype) { {
		set("valueOf", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				if (ths instanceof JSBoolean)
					return ((JSBoolean) ths).value;
				return false;
			}
		});
	} };
	
	final JSObject regexpPrototype = new JSObject(objectPrototype) { {
		set("test", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Pattern pattern = ((JSRegExp) ths).getPattern();
				return pattern.matcher(JSUtils.asString(l0)).find();
			}
		});
		
		set("exec", new JSFunction (functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Pattern pattern = ((JSRegExp) ths).getPattern();
				JSArray out = new JSArray(getArrayPrototype(), 0);
				Matcher matcher = pattern.matcher(JSUtils.asString(l0));
				if (!((JSRegExp) ths).isGlobal()) {
					if (!matcher.find())
						return JSNull.NULL;
					for (int i = 0; i < matcher.groupCount() + 1; i++)
						out.push(matcher.group(i));
				} else {
					while (matcher.find())
						out.push(matcher.group(0));
					if (out.getLength() == 0)
						return JSNull.NULL;
				}
				return out;
			}
		});
	} };
	
	/*
	 * objects/constructors
	 */	
	
	final JSFunction requireFunction = new JSFunction(functionPrototype) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			return Modules.getModule(JSUtils.asString(l0)).load();
		}
	};
	
	final JSObject exportsObject = new JSObject(objectPrototype) { };
	
	final JSFunction printFunction = new JSFunction(functionPrototype) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			for (int i = 0; i < arguments.length; i++) {
				if (i > 0)
					System.out.print(" ");
				System.out.print(JSUtils.asString(arguments[i]));
			}
			System.out.println("");
			return null;
		}
	};
	
	final JSObject mathObject = new JSObject(objectPrototype) { {
		set("abs", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.abs(JSUtils.asNumber(l0));
			}
		});
		
		set("pow", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.pow(JSUtils.asNumber(l0), JSUtils.asNumber(l1));
			}
		});
		
		set("sqrt", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.sqrt(JSUtils.asNumber(l0));
			}
		});
		
		set("max", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.max(JSUtils.asNumber(l0), JSUtils.asNumber(l1));
			}
		});
		
		set("floor", new JSFunction(functionPrototype) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return Math.floor(JSUtils.asNumber(l0));
			}
		});
	} };
	
	final JSFunction numberConstructor = new JSFunction(functionPrototype) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			return JSUtils.asNumber(l0);
		}
	};
	
	final JSFunction arrayConstructor = new JSFunction(functionPrototype) {
		public Object instantiate(int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return invoke(null, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		}
		
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			
			// single-argument constructor
			if (argc == 1) {
				int length = (int) JSUtils.asNumber(l0);
				JSArray arr = new JSArray(arrayPrototype, length);
				for (int i = 0; i < length; i++)
					arr.push(null);
				return arr;
			}
			
			// literal declaration
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			JSArray arr = new JSArray(arrayPrototype, arguments.length);
			for (int i = 0; i < arguments.length; i++)
				arr.push(arguments[i]);
			return arr;
		}
	};
	
	/*
	 * prototype accessors
	 */
	
	public JSObject getObjectPrototype() {
		return objectPrototype;
	}
	
	public JSObject getArrayPrototype() {
		return arrayPrototype;
	}
	
	public JSObject getStringPrototype() {
		return stringPrototype;
	}
	
	public JSObject getNumberPrototype() {
		return numberPrototype;
	}
	
	public JSObject getBooleanPrototype() {
		return booleanPrototype;
	}
	
	public JSObject getFunctionPrototype() {
		return functionPrototype;
	}
	
	public JSObject getRegExpPrototype() {
		return regexpPrototype;
	}
	
	/*
	 * scope accessors
	 */
	
	Object _require = requireFunction;
	public Object get_require() { return _require; }
	public void set_require(Object value) { _require = value; }
	
	Object _exports = exportsObject;
	public Object get_exports() { return _exports; }
	public void set_exports(Object value) { _exports = value; }
	
	Object _print = printFunction;
	public Object get_print() { return _print; }
	public void set_print(Object value) { _print = value; }
	
	Object _Math = mathObject;
	public Object get_Math() { return _Math; }
	public void set_Math(Object value) { _Math = value; }

	Object _Array = arrayConstructor;
	public Object get_Array() { return _Array; }
	public void set_Array(Object value) { _Array = value; }
	
	Object _Number = numberConstructor;
	public Object get_Number() { return _Number; }
	public void set_Number(Object value) { _Number = value; }
}
