
# Example


---

## Generating Miniaudio Bindings

This section describes how to generate bindings for
[miniaudio](https://github.com/mackron/miniaudio) using the binding generator.

### Basic Command

```shell
java src/Main.java \
  '--header=/path/to/miniaudio.h' \
  '--args=-I;/usr/include' \
  '--enable-macro=true' \
  '--out=miniaudio-out/src' \
  '--pkg-name=libminiaudio' \
  '--name=MiniAudio' \
  '--filter-decl=ma.*'
```

> **Note**
> Some arguments contain characters that are interpreted by the shell.
> It is recommended to wrap each argument in quotes when using **bash**.

---

## Argument Details

### `--header=/path/to/miniaudio.h`

Path to the main `miniaudio.h` header file that will be parsed by Clang.

---

### `--args=-I;/usr/include`

Extra arguments passed directly to
`clang_parseTranslationUnit2`.

* The tool uses **`;` (semicolon)** as the internal separator,
  so multiple arguments can be specified:

  ```text
  --args=-I;/usr/include;-DMY_DEFINE
  ```
* Because `;` has special meaning in **bash**, this option **must be quoted**:

  ```shell
  '--args=-I;/usr/include;-DMY_DEFINE'
  ```

Typical use cases:

* Adding include directories (`-I`)
* Defining macros (`-D`)
* Passing Clang-specific flags

---

### `--enable-macro=true`

Enables generation of macro-based constants

Without this option, macro definitions will be ignored.

---

### `--out=miniaudio-out/src`

Output directory for the generated source files.

---

### `--pkg-name=libminiaudio`

Specifies the **package name** of the generated bindings.

Example (Java):

```java
package libminiaudio;
```

---

### `--name=MiniAudio`

Specifies the **base name of generated files**.

Examples:

* `MiniAudioFunctionSymbols.java`
* `MiniAudioMacros.java`
* `MiniAudioConstants.java`

---

### `--filter-decl=ma.*`

Filters declarations by **symbol name**.

Only functions, types, macros, and constants whose names start with `ma`
will be generated.

This is useful to:

* Avoid pulling in unrelated system or helper symbols
* Keep the output strictly Miniaudio-specific

---

### `--filter-header=...`

Filters declarations by **header file path**.

* This option matches against the **absolute header path**
* Internally implemented as:

  ```java
  Pattern.compile(...).asMatchPredicate()
  ```
* Therefore, the pattern usually needs to include **directory prefixes**

Example:

```shell
'--filter-header=.*/miniaudio.h'
```

This is especially useful when:

* Multiple headers are transitively included
* You want to restrict output to a specific header file

---

### `--greedy`

Forces the generator to emit **all types defined in the header**,
instead of only types referenced by exported symbols.

Recommended when:

* You want full type coverage
* You want to avoid missing structs, enums, or typedefs

---

## Recommended Command

```shell
java src/Main.java \
  '--header=/path/to/miniaudio.h' \
  '--args=-I;/usr/include' \
  '--enable-macro=true' \
  '--greedy' \
  '--out=miniaudio-out/src' \
  '--pkg-name=libminiaudio' \
  '--name=MiniAudio' \
  '--filter-decl=ma.*' \
  '--filter-header=.*miniaudio.h'
```

---

Using miniaudio to play the music file

```java
import libminiaudio.MiniAudioFunctionSymbols;
import libminiaudio.MiniAudioSymbolProvider;
import libminiaudio.aggregates.ma_engine;
import libminiaudio.aggregates.ma_engine_config;
import libminiaudio.aggregates.ma_log;
import libminiaudio.common.FunctionUtils;
import libminiaudio.common.Ptr;
import libminiaudio.common.PtrI;
import libminiaudio.common.Str;
import libminiaudio.enumerates.ma_result;
import libminiaudio.functions.ma_log_callback_proc;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.Scanner;

public class Main {
    public static void main() {

        MiniAudioSymbolProvider.symbolProvider = name ->
                SymbolLookup.libraryLookup("/path/to/libminiaudio.so",
                        Arena.global()).find(name).map(FunctionUtils.FunctionSymbol::new);

        Arena mem = Arena.ofConfined();

        Ptr<ma_engine_config> ma_engine_config = MiniAudioFunctionSymbols.ma_engine_config_init(mem);

        Ptr<ma_log> log = ma_log.ptr(mem);
        MiniAudioFunctionSymbols.ma_log_init(PtrI.of(MemorySegment.NULL), log);
        MiniAudioFunctionSymbols.ma_log_register_callback(log,
                MiniAudioFunctionSymbols.ma_log_callback_init(ma_log_callback_proc.of(mem,
                        (pUserData, level, pMessage)
                                -> System.out.print(new Str(MiniAudioFunctionSymbols.ma_log_level_to_string(level)) + " : " + new Str(pMessage).get())), PtrI.of(MemorySegment.NULL)));
        ma_engine_config.pte().pLog(log);

        Ptr<ma_engine> engine = ma_engine.ptr(mem);
        ma_result result = MiniAudioFunctionSymbols.ma_engine_init(ma_engine_config, engine);
        if (!ma_result.MA_SUCCESS.equals(result)) {
            System.out.println("Failed");
            return;
        }

        MiniAudioFunctionSymbols.ma_engine_play_sound(engine, new Str(mem, "music.flac"), PtrI.of(MemorySegment.NULL));
        new Scanner(System.in).nextLine();
        MiniAudioFunctionSymbols.ma_engine_uninit(engine);
    }
}
```

## Generating the Vulkan and [VMA](https://github.com/GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator) Bindings

```shell
java src/Main.java \
'--header=/usr/include/vulkan/vulkan.h' \
'--args=-I;/usr/include' \
'--enable-macro=true' \
'--out=vulkan-out/src' \
'--pkg-name=vk' \
'--name=Vulkan' \
'--filter-decl=(vk.*|VK_.*|Vk.*)' \
'--next-component' \
'--header=/usr/include/vk_mem_alloc.h' \
'--args=-I;/usr/include' \
'--enable-macro=false' \
'--out=vma-out/src' \
'--pkg-name=vma' \
'--name=Vma' \
'--filter-decl=vma.*'
```

Build libvma.so

```shell
echo '#define VMA_IMPLEMENTATION

#if defined(_WIN32)
#define VMA_CALL_PRE extern __declspec(dllexport)
#else
#define VMA_CALL_PRE extern __attribute__((visibility("default")))
#endif

#include <vulkan/vulkan.h>
#include <cstdio>
#include <vk_mem_alloc.h>' > vma.cpp
g++ vma.cpp -fPIC -shared -DVMA_STATIC_VULKAN_FUNCTIONS=0 -DVMA_DYNAMIC_VULKAN_FUNCTIONS=1 -o libvma.so
```

Using VMA to allocate 1024 MiB GPU memory

```java
import vk.VulkanSymbolProvider;
import vk.aggregates.VkApplicationInfo;
import vk.aggregates.VkBufferCreateInfo;
import vk.aggregates.VkDeviceCreateInfo;
import vk.aggregates.VkInstanceCreateInfo;
import vk.common.*;
import vk.enumerates.VkBufferUsageFlagBits;
import vk.functions.PFN_vkGetDeviceProcAddr;
import vk.functions.PFN_vkGetInstanceProcAddr;
import vk.values.VkBuffer;
import vk.values.VkInstance;
import vk.values.VkPhysicalDevice;
import vk.values.uint32_t;
import vma.VmaSymbolProvider;
import vma.aggregates.VmaAllocationCreateInfo;
import vma.aggregates.VmaAllocatorCreateInfo;
import vma.aggregates.VmaVulkanFunctions;
import vma.values.VmaAllocation;
import vma.values.VmaAllocator;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.Optional;
import java.util.Scanner;

import static vk.VulkanFunctionSymbols.*;
import static vk.VulkanMacros.VK_API_VERSION_1_0;
import static vk.enumerates.VkBufferUsageFlagBits.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static vk.enumerates.VkResult.VK_SUCCESS;
import static vk.enumerates.VkSharingMode.VK_SHARING_MODE_EXCLUSIVE;
import static vk.enumerates.VkStructureType.*;
import static vma.VmaFunctionSymbols.vmaCreateAllocator;
import static vma.VmaFunctionSymbols.vmaCreateBuffer;
import static vma.enumerates.VmaMemoryUsage.VMA_MEMORY_USAGE_CPU_TO_GPU;


public class Main {
    public static void main(String[] args) {
        VulkanSymbolProvider.symbolProvider = name -> Optional.of(new FunctionUtils.FunctionSymbol(
                SymbolLookup.libraryLookup("libvulkan.so", Arena.global()).find(name).orElseThrow()));
        VmaSymbolProvider.symbolProvider = name -> Optional.of(new FunctionUtils.FunctionSymbol(
                SymbolLookup.libraryLookup("/path/to/libvma.so", Arena.global()).find(name).orElseThrow()));

        Arena mem = Arena.ofConfined();
        Ptr<VkApplicationInfo> appInfo = VkApplicationInfo.ptr(mem);
        appInfo.apply(i -> {
            i.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            i.pApplicationName(new Str(mem, "Vulkan Memory Allocation Example"));
            i.applicationVersion(I32I.of(1));
            i.pEngineName(new Str(mem, "No Engine"));
            i.engineVersion(I32I.of(1));
            i.apiVersion(I32I.of(VK_API_VERSION_1_0));
        });

        Ptr<VkInstanceCreateInfo> createInfo = VkInstanceCreateInfo.ptr(mem);
        createInfo.apply(i -> {
            i.sType(I32I.of(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO));
            i.pApplicationInfo(appInfo);
        });

        VkInstance instance = VkInstance.ptr(mem).self(i -> {
            if (!vkCreateInstance(createInfo, PtrI.of(MemorySegment.NULL), i).equals(VK_SUCCESS)) {
                throw new RuntimeException("Failed to create Vulkan instance!");
            }
        }).pte();

        // Create a Vulkan device
        Ptr<vk.values.VkDevice> device = vk.values.VkDevice.ptr(mem);

        // Enumerate physical devices and pick the first one
        Ptr<uint32_t> deviceCount = uint32_t.ptr(mem);
        vkEnumeratePhysicalDevices(instance, deviceCount, PtrI.of(MemorySegment.NULL));

        if (deviceCount.pte().value() == 0) {
            throw new RuntimeException("Failed to find a GPU with Vulkan support!");
        }

        VkPhysicalDevice physicalDevice = VkPhysicalDevice.ptr(mem).self(
                i -> vkEnumeratePhysicalDevices(instance, deviceCount, i)).pte();

        // Create the logical device
        Ptr<VkDeviceCreateInfo> deviceCreateInfo = VkDeviceCreateInfo.ptr(mem);
        deviceCreateInfo.pte().sType(I32I.of(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO));

        if (!vkCreateDevice(physicalDevice, deviceCreateInfo, PtrI.of(MemorySegment.NULL), device).equals(VK_SUCCESS)) {
            throw new RuntimeException("Failed to create Vulkan device!");
        }

        Ptr<VkBuffer> buffer = VkBuffer.ptr(mem);

        var vmaVkFuncs = VmaVulkanFunctions.ptr(mem).apply(i -> {
            i.vkGetInstanceProcAddr(new PFN_vkGetInstanceProcAddr(VulkanSymbolProvider.symbolProvider.provide("vkGetInstanceProcAddr").orElseThrow().getSymbol()));
            i.vkGetDeviceProcAddr(new PFN_vkGetDeviceProcAddr(VulkanSymbolProvider.symbolProvider.provide("vkGetDeviceProcAddr").orElseThrow().getSymbol()));
        });

        var allocatorInfo = VmaAllocatorCreateInfo.ptr(mem);
        allocatorInfo.pte().physicalDevice(physicalDevice).device(device.pte())
                .instance(instance).pVulkanFunctions(vmaVkFuncs);

        Ptr<VmaAllocator> allocator = VmaAllocator.ptr(mem);
        if (!vmaCreateAllocator(allocatorInfo, allocator).equals(VK_SUCCESS)) {
            throw new RuntimeException("Failed to create VMA allocator!");
        }

        Ptr<VmaAllocation> allocation = VmaAllocation.ptr(mem);
        createBuffer(allocator.pte(), 1024 * 1024 * 1024, VK_BUFFER_USAGE_TRANSFER_DST_BIT, buffer, allocation);
        System.out.println("Allocated 1024 MiB GPU memory");
        System.out.println("Press enter to exit...");
        new Scanner(System.in).nextLine();
    }

    static void createBuffer(VmaAllocator allocator, long size, VkBufferUsageFlagBits usage,
                             Ptr<VkBuffer> buffer, Ptr<VmaAllocation> allocation) {
        Arena mem = Arena.ofConfined();
        Ptr<VkBufferCreateInfo> bufferInfo = VkBufferCreateInfo.ptr(mem);
        bufferInfo.pte().sType(I32I.of(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO))
                .size(I64I.of(size))
                .usage(I32I.of(usage))
                .sharingMode(I32I.of(VK_SHARING_MODE_EXCLUSIVE));
        Ptr<VmaAllocationCreateInfo> allocInfo = VmaAllocationCreateInfo.ptr(mem);
        allocInfo.pte().usage(VMA_MEMORY_USAGE_CPU_TO_GPU);

        if (!vmaCreateBuffer(allocator, bufferInfo, allocInfo, buffer, allocation, PtrI.of(MemorySegment.NULL)).equals(VK_SUCCESS)) {
            throw new RuntimeException("Failed to create buffer with VMA!");
        }
        mem.close();
    }
}
```
