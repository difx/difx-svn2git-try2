#include <libudev.h>
#include <stdio.h>
#include <stdlib.h>
#include <locale.h>
#include <unistd.h>

main ()
{
        int fd;         // file descriptor for the udev monitor
        struct udev *udev;
        struct udev_monitor *mon;


	udev = udev_new();
        if (!udev) {
                printf("Can't create udev\n");
                exit(1);
        }
        mon = udev_monitor_new_from_netlink(udev, "udev");
        udev_monitor_filter_add_match_subsystem_devtype(mon, "block", NULL);
        udev_monitor_enable_receiving(mon);
        /* Get the file descriptor (fd) for the monitor.
 *  *  *         This fd will get passed to select() */
        fd = udev_monitor_get_fd(mon);

}
