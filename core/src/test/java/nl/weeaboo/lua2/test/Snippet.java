package nl.weeaboo.lua2.test;

public class Snippet {

    /*
    TODO: If the key is weak, but the value isn't, we maintain a hard reference to the value until this WeakKeySlot is removed from the table.
          That requires iterating over the table using pairs/next
          How can I make the lifetime of value depend on that of key without the above issue?
    TODO: This is obscure as fuck -- add a dedicated unit test.
    */
}

