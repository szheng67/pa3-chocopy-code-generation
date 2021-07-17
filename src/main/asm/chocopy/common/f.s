addi sp, sp, -@f.size                    # Reserve space for stack frame.
sw ra, @f.size-4(sp)                     # return address
sw fp, @f.size-8(sp)                     # control link
addi fp, sp, @f.size                     # New fp is at old SP.
li a0, 1                                 # Load integer literal 1
sw a0, -12(fp)                           # local variable x
lw a0, -12(fp)                           # Load var: f.x
j label_2                                # Go to return
mv a0, zero                              # Load None
j label_2                                # Jump to function epilogue